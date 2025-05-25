import java.util.Arrays;
import java.util.ArrayList;

public class Verdipapir {
    private ArrayList<ArrayList<Object>> transaksjoner = new ArrayList<>();
    private final String navn;
    private String ticker = "";
    private Double gav = 0.0;
    private Double antall = 0.0;
    private Double utbytte = 0.0;
    private Double realisertAvkastning = 0.0;

    public Verdipapir(String n) {
        navn = n;
        settTicker();
    }

    private void settTicker() {
        ticker = "HAUTO.OL";
    }

    public String hentNavn() { return navn; }
    public String hentTicker() { return ticker; }

    public double hentAntall() { return Double.parseDouble(String.format("%.2f", antall).replace(",", ".")); }
    public double hentGav() { return Double.parseDouble(String.format("%.2f", gav).replace(",", ".")); }
    public double hentUtbytte() { return Double.parseDouble(String.format("%.2f", utbytte).replace(",", ".")); }
    public double hentRealisertAvkastning() { return Double.parseDouble(String.format("%.2f", realisertAvkastning).replace(",", ".")); }

    private void oppdaterAntall(double a) { antall = Math.max(antall + a, 0); }

    public void leggTilUtbytte(double belop) { utbytte += belop; }

    public void leggTilTransaksjon(double verdi, double antall, double kurs, double resultat, double totaleAvgifter) {
        if (verdi < 0) {
            transaksjoner.add(new ArrayList<>(Arrays.asList("KJØPT", ((antall * kurs) + totaleAvgifter))));
            oppdaterAntall(antall);
        } else {
            transaksjoner.add(new ArrayList<>(Arrays.asList("SALG", ((antall * kurs) + totaleAvgifter))));
            oppdaterAntall(-antall);
            realisertAvkastning += resultat;
        }
    }

    public void beregnGAV() {    
        double total = 0.0;

        for (ArrayList<Object> trans : transaksjoner) {
            double belop = (double) trans.get(1);

            if (trans.get(0).equals("KJØPT")) {
                total += belop;
            } else {
                total -= belop;
            } 
        }
    
        if (antall > 0) {
            gav =  total / antall;
        } else {
            gav = 0.0;
        }
    }
}