#!/usr/bin/env bash
# Compile against the user's installed 26.2 dependencies; no global Java changes.
set -euo pipefail
cd "$(dirname "$0")"
prism="${PRISM_ROOT:-/home/ari/.local/share/PrismLauncher}"
task_jdk="${BAZAAR_JDK:-/usr/lib/jvm/java-26-openjdk}"
task_build="$(mktemp -d /tmp/bazaar-compile.XXXXXX)"
mkdir -p "$task_build/classes" "$task_build/api" "$task_build/resources" "$task_build/tests" build/libs
unzip -q "$prism/instances/26.2/minecraft/mods/fabric-api-0.161.0+26.2.jar" 'META-INF/jars/*.jar' -d "$task_build/api"
task_cp="$task_build/api/META-INF/jars/*"
for dependency in \
    net/fabricmc/fabric-loader/0.19.5/fabric-loader-0.19.5.jar \
    com/mojang/minecraft/26.2/minecraft-26.2-client.jar \
    com/mojang/brigadier/1.3.11/brigadier-1.3.11.jar \
    com/google/code/gson/gson/2.14.0/gson-2.14.0.jar \
    org/lwjgl/lwjgl-glfw/3.4.1/lwjgl-glfw-3.4.1.jar; do
    test -f "$prism/libraries/$dependency"
    task_cp="$task_cp:$prism/libraries/$dependency"
done
mapfile -t task_sources < <(rg --files src/main/java -g '*.java')
"$task_jdk/bin/javac" --release 25 -encoding UTF-8 -cp "$task_cp" -d "$task_build/classes" "${task_sources[@]}"
"$task_jdk/bin/javac" --release 25 -cp "$task_build/classes" -d "$task_build/tests" src/test/java/com/example/bazaarpredictor/OpportunityTableTest.java
"$task_jdk/bin/java" -cp "$task_build/classes:$task_build/tests" com.example.bazaarpredictor.OpportunityTableTest
cp -r src/main/resources/. "$task_build/resources/"
version="$(sed -n 's/^mod_version=//p' gradle.properties)"
sed -i 's/${version}/'"$version"'/g' "$task_build/resources/fabric.mod.json"
"$task_jdk/bin/jar" --create --file "build/libs/bazaar-predictor-$version.jar" -C "$task_build/classes" . -C "$task_build/resources" .
printf 'Built %s\n' "$PWD/build/libs/bazaar-predictor-$version.jar"
