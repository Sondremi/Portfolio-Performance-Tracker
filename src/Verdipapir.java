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
        ticker = "HAUTO.OL"; // Dummy
    }

    public String hentNavn() { return navn; }
    public String hentTicker() { return ticker; }
    public String hentAntall() { return String.format("%.2f", antall); }
    public String hentGav() { return String.format("%.2f", gav); }
    public String hentUtbytte() { return String.format("%.2f", utbytte); }
    public String hentRealisertAvkastning() { return String.format("%.2f", realisertAvkastning); }

    public void leggTilUtbytte(double belop) { utbytte += belop; }

    public void leggTilTransaksjon(double verdi, double oppdatertAntall, double kurs, double resultat, double totaleAvgifter) {
        if (verdi < 0) {
            transaksjoner.add(new ArrayList<>(Arrays.asList("KJØP", ((oppdatertAntall * kurs) + totaleAvgifter))));
            antall += oppdatertAntall;
        } else {
            transaksjoner.add(new ArrayList<>(Arrays.asList("SALG", ((oppdatertAntall * kurs) + totaleAvgifter))));
            antall -= oppdatertAntall;
            realisertAvkastning += resultat;
        }
    }

    public void beregnGAV() {    
        double total = 0.0;

        for (ArrayList<Object> trans : transaksjoner) {
            double belop = (double) trans.get(1);

            if (trans.get(0).equals("KJØP")) {
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