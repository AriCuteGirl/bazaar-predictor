package com.example.bazaarpredictor;
import com.example.bazaarpredictor.model.*;
import java.util.*;

public final class OpportunityTableTest {
    private static final Opportunity A = row("A", 900, 3000, 500, 10);
    private static final Opportunity B = row("B", 2000, 2500, 200, 20);
    private static final Opportunity C = row("C", 10, 100, 50, 30);
    private static Opportunity row(String id, double buy, double sell, double net, long time) {
        return new Opportunity(id, id, buy, sell, net, 20, 1000, 0, time, "ok");
    }
    private static void update(OpportunityTable t) { t.update(List.of(A,B,C), "", 0, 0, 100, Set.of(), false, 2); }
    private static void expect(boolean b, String message) { if (!b) throw new AssertionError(message); }
    private static void ids(OpportunityTable t, String expected) {
        expect(t.rows().stream().map(Opportunity::productId).reduce("", String::concat).equals(expected), expected);
    }
    public static void main(String[] args) {
        var t = new OpportunityTable(); update(t); ids(t, "ABC");
        t.sortBy(OpportunityTable.Column.BUY); update(t); ids(t, "CAB");
        t.selectVisibleRow(1, 2); expect(t.selected().productId().equals("A"), "select sorted row");
        t.sortBy(OpportunityTable.Column.BUY); update(t); ids(t, "BAC");
        expect(t.selected().productId().equals("A"), "stable selection after sort");
        t.sortBy(OpportunityTable.Column.SELL); update(t); ids(t, "CBA");
        t.sortBy(OpportunityTable.Column.SELL); update(t); ids(t, "ABC");
        t.scrollTo(1, 2); t.selectVisibleRow(1, 2);
        expect(t.selected().productId().equals("C"), "scrolled selection");
        t.update(List.of(A,B,C), "B", 0, 0, 100, Set.of(), false, 2);
        expect(t.selected() == null && t.offset() == 0, "filter clears hidden selection and clamps scroll");
        t.selectVisibleRow(0, 2); expect(t.selected().productId().equals("B"), "filtered selection");
        var changedB = row("B", 5000, 9000, 3000, 100);
        t.update(List.of(A,changedB,C), "", 0, 0, 100, Set.of(), false, 2);
        expect(t.selected() == changedB, "refresh replaces selected data by ID");
        t.update(List.of(A,B,C), "", 0, 0, 100, Set.of("C"), true, 2); ids(t,"C");
        t.update(List.of(), "", 0, 0, 100, Set.of(), false, 2);
        expect(t.selected() == null && t.atVisibleRow(0,2) == null, "empty table");
        t.sortBy(OpportunityTable.Column.AGE); update(t); ids(t,"CBA");
        t.sortBy(OpportunityTable.Column.AGE); update(t); ids(t,"ABC");
        System.out.println("PASS: numeric buy/sell sorting both directions, stable selection, filtering, scrolling, stars, live replacement, empty list and age sorting");
    }
}
