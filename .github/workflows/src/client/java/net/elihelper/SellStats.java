package net.elihelper;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Zaehlt Verkaufs-Auszahlungen aus dem Chat (nur lesen, keine Automatisierung). */
public final class SellStats {
    public static final class Event {
        public long time; public double amount;
        public Event() {}
        Event(long t, double a) { time = t; amount = a; }
    }
    public static final class TestResult {
        public double amount; public double multi; public long time;
    }
    private static final class Saved {
        double allTime;
        List<Event> events = new ArrayList<>();
        List<TestResult> tests = new ArrayList<>();
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("elihelper-stats.json");
    private static final long DAY = 24L * 60 * 60 * 1000;

    private static Saved saved = new Saved();
    private static Pattern pattern;
    private static String patternSource = "";
    private static long lastSave = 0;

    public static double session = 0;
    private static long firstSessionEvent = 0;

    public static boolean testRunning = false;
    public static int testTicksLeft = 0;
    public static double testSum = 0;
    public static double testMulti = 1.0;

    private SellStats() {}

    public static void load() {
        try {
            if (Files.exists(FILE)) {
                Saved s = GSON.fromJson(Files.readString(FILE), Saved.class);
                if (s != null) saved = s;
            }
        } catch (IOException | RuntimeException e) {
            EliHelperClient.LOGGER.warn("Statistik konnte nicht gelesen werden", e);
        }
        if (saved.events == null) saved.events = new ArrayList<>();
        if (saved.tests == null) saved.tests = new ArrayList<>();
        prune();
    }

    public static void save() {
        try {
            prune();
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, GSON.toJson(saved));
        } catch (IOException e) {
            EliHelperClient.LOGGER.warn("Statistik konnte nicht gespeichert werden", e);
        }
    }

    private static void prune() {
        long cutoff = System.currentTimeMillis() - DAY;
        saved.events.removeIf(e -> e.time < cutoff);
        while (saved.tests.size() > 10) saved.tests.remove(0);
    }

    private static Pattern pattern() {
        String src = EliConfig.get().payoutRegex;
        if (pattern == null || !src.equals(patternSource)) {
            try {
                pattern = Pattern.compile(src);
            } catch (RuntimeException e) {
                EliHelperClient.LOGGER.warn("payoutRegex ungueltig: {}", src);
                pattern = Pattern.compile("(?!x)x"); // matcht nie
            }
            patternSource = src;
        }
        return pattern;
    }

    public static void onChat(String text) {
        if (!EliConfig.get().statsEnabled) return;
        try {
            Matcher m = pattern().matcher(text);
            if (!m.find()) return;
            double amount = parseAmount(m.group("amount"));
            if (amount <= 0) return;
            long now = System.currentTimeMillis();
            saved.allTime += amount;
            saved.events.add(new Event(now, amount));
            session += amount;
            if (firstSessionEvent == 0) firstSessionEvent = now;
            if (testRunning) testSum += amount;
        } catch (RuntimeException ignored) {
            // z.B. fehlende Gruppe "amount" in der Regex
        }
    }

    public static void tick() {
        if (testRunning && --testTicksLeft <= 0) finishTest();
        long now = System.currentTimeMillis();
        if (now - lastSave > 30_000) { lastSave = now; save(); }
    }

    public static void startTest(double multi) {
        testMulti = multi <= 0 ? 1.0 : multi;
        testSum = 0;
        testTicksLeft = 20 * 60;
        testRunning = true;
    }

    public static void cancelTest() { testRunning = false; }

    private static void finishTest() {
        testRunning = false;
        TestResult r = new TestResult();
        r.amount = testSum; r.multi = testMulti; r.time = System.currentTimeMillis();
        saved.tests.add(r);
        save();
    }

    public static TestResult lastTest() {
        return saved.tests.isEmpty() ? null : saved.tests.get(saved.tests.size() - 1);
    }

    public static void resetSession() { session = 0; firstSessionEvent = 0; }

    public static double allTime() { return saved.allTime; }

    public static double last24h() {
        long cutoff = System.currentTimeMillis() - DAY;
        double sum = 0;
        for (Event e : saved.events) if (e.time >= cutoff) sum += e.amount;
        return sum;
    }

    /** Gleitender Schnitt der letzten 5 Minuten; NaN bis 2 Minuten Daten vorliegen. */
    public static double perHour() {
        if (firstSessionEvent == 0) return Double.NaN;
        long now = System.currentTimeMillis();
        double minutes = Math.min(5.0, (now - firstSessionEvent) / 60000.0);
        if (minutes < 2.0) return Double.NaN;
        long cutoff = now - 5 * 60_000L;
        double sum = 0;
        for (Event e : saved.events) if (e.time >= cutoff) sum += e.amount;
        return sum / minutes * 60.0;
    }

    /** Hochrechnung: Basis = Auszahlung / Multiplikator (der Multi wird nicht doppelt gezaehlt). */
    public static double projectPerMinute(TestResult r, double targetMulti) {
        return r.amount / r.multi * targetMulti;
    }

    public static double parseAmount(String raw) {
        String s = raw.trim().replace(" ", "").toLowerCase();
        double factor = 1;
        if (s.endsWith("k")) { factor = 1e3; s = s.substring(0, s.length() - 1); }
        else if (s.endsWith("m")) { factor = 1e6; s = s.substring(0, s.length() - 1); }
        else if (s.endsWith("b")) { factor = 1e9; s = s.substring(0, s.length() - 1); }
        int dot = s.lastIndexOf('.'), comma = s.lastIndexOf(',');
        if (dot >= 0 && comma >= 0) {
            char dec = dot > comma ? '.' : ',';
            char thou = dec == '.' ? ',' : '.';
            s = s.replace(String.valueOf(thou), "").replace(dec, '.');
        } else if (dot >= 0 || comma >= 0) {
            char sep = dot >= 0 ? '.' : ',';
            int idx = s.lastIndexOf(sep);
            boolean thousands = factor == 1 && s.length() - idx - 1 == 3 && s.indexOf(sep) == idx;
            boolean multi = s.indexOf(sep) != idx;
            s = (thousands || multi) ? s.replace(String.valueOf(sep), "") : s.replace(sep, '.');
        }
        return Double.parseDouble(s) * factor;
    }

    /** 1.4K, 12M, 3.1B - immer aufgerundet. */
    public static String fmt(double v) {
        if (Double.isNaN(v)) return "-";
        String[] suf = {"", "K", "M", "B", "T"};
        int i = 0;
        while (Math.abs(v) >= 1000 && i < suf.length - 1) { v /= 1000; i++; }
        double up = Math.ceil(v * 10) / 10.0;
        String num = up == Math.floor(up) ? String.valueOf((long) up) : String.valueOf(up);
        return num + suf[i];
    }
}
