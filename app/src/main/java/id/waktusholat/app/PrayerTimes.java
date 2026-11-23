package id.waktusholat.app;

/** Hitung waktu sholat (metode Kemenag RI: Subuh 20°, Isya 18°, ihtiyat +2 menit), tanpa internet. */
public final class PrayerTimes {
    private PrayerTimes() {}

    public static final String[] IDS = {"fajr", "dhuhr", "asr", "maghrib", "isha"};
    public static final String[] NAMES = {"Subuh", "Dzuhur", "Ashar", "Maghrib", "Isya"};

    private static double rad(double d) { return Math.toRadians(d); }
    private static double deg(double r) { return Math.toDegrees(r); }
    private static double fix(double a, double m) { a = a - m * Math.floor(a / m); return a < 0 ? a + m : a; }

    private static double julian(int y, int m, int d) {
        if (m <= 2) { y -= 1; m += 12; }
        double a = Math.floor(y / 100.0);
        double b = 2 - a + Math.floor(a / 4);
        return Math.floor(365.25 * (y + 4716)) + Math.floor(30.6001 * (m + 1)) + d + b - 1524.5;
    }

    /** @return jam lokal (desimal) untuk {Subuh, Dzuhur, Ashar, Maghrib, Isya}; NaN bila tidak terdefinisi. */
    public static double[] compute(int y, int m, int d, double lat, double lng, double tz) {
        double jd = julian(y, m, d) - lng / 360.0 + 0.5;
        double D = jd - 2451545.0;
        double g = fix(357.529 + 0.98560028 * D, 360);
        double q = fix(280.459 + 0.98564736 * D, 360);
        double L = fix(q + 1.915 * Math.sin(rad(g)) + 0.020 * Math.sin(rad(2 * g)), 360);
        double e = 23.439 - 0.00000036 * D;
        double ra = fix(deg(Math.atan2(Math.cos(rad(e)) * Math.sin(rad(L)), Math.cos(rad(L)))) / 15, 24);
        double eqt = q / 15 - ra;
        double decl = deg(Math.asin(Math.sin(rad(e)) * Math.sin(rad(L))));
        double noon = 12 - eqt;

        double asrAngle = -deg(Math.atan(1.0 / (1 + Math.tan(rad(Math.abs(lat - decl))))));
        double[] r = new double[5];
        r[0] = noon - angleT(20, lat, decl);
        r[1] = noon;
        r[2] = noon + angleT(asrAngle, lat, decl);
        r[3] = noon + angleT(0.833, lat, decl);
        r[4] = noon + angleT(18, lat, decl);
        double off = tz - lng / 15 + 2.0 / 60; // ihtiyat 2 menit
        for (int i = 0; i < 5; i++) r[i] += off;
        return r;
    }

    private static double angleT(double angle, double lat, double decl) {
        double x = (-Math.sin(rad(angle)) - Math.sin(rad(decl)) * Math.sin(rad(lat)))
                / (Math.cos(rad(decl)) * Math.cos(rad(lat)));
        if (x > 1 || x < -1) return Double.NaN;
        return deg(Math.acos(x)) / 15;
    }
}
