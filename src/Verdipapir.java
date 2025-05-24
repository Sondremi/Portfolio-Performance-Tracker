import java.util.Arrays;
import java.util.ArrayList;

public class Verdipapir {
    private ArrayList<ArrayList<Object>> transaksjoner = new ArrayList<>();
    private final String navn;
    private String ticker = "";
    private Double gav = 0.0;
    private Double antall = 0.0;
    private Double utbytte = 0.0;
    private Double kostpris = 0.0;
    private Double markedsverdi = 0.0;
    private Double totalAvkastning = 0.0;
    private Double realisertAvkastning = 0.0;
    private Double urealisertAvkastning = 0.0;
    
    public Verdipapir(String n) {
        navn = n;
        settTicker();
    }

    // Gjør om navn til ticker med yahoo finance
    // Sjekk om det er aksje eller fond
    // For å finne ticker eller ISIN
    private void settTicker() {
        ticker = "KOA.OL";
    }

    // Metoder for å hente verdier fra verdipapiret
    public String hentNavn() { return navn; }
    public String hentTicker() { return ticker; }
    public ArrayList<ArrayList<Object>> hentTransaksjoner() { return transaksjoner; }
    public double hentGav() { return Double.parseDouble(String.format("%.2f", gav).replace(",", ".")); }
    public double hentUtbytte() { return Double.parseDouble(String.format("%.2f", utbytte).replace(",", ".")); }
    public double hentKostpris() { return Double.parseDouble(String.format("%.2f", kostpris).replace(",", ".")); }
    public double hentAntall() { return Double.parseDouble(String.format("%.2f", antall).replace(",", ".")); }
    public double hentMarkedsverdi() { return Double.parseDouble(String.format("%.2f", markedsverdi).replace(",", ".")); }
    public double hentTotalAvkastning() { return Double.parseDouble(String.format("%.2f", totalAvkastning).replace(",", ".")); }
    public double hentRealisertAvkastning() { return Double.parseDouble(String.format("%.2f", realisertAvkastning).replace(",", ".")); }
    public double hentUrealisertAvkastning() { return Double.parseDouble(String.format("%.2f", urealisertAvkastning).replace(",", ".")); }

    public void beregnData() {
        beregnGAV();
        kostpris = gav * antall;
        markedsverdi = -1.0;
        urealisertAvkastning = markedsverdi - kostpris;
        totalAvkastning = urealisertAvkastning + realisertAvkastning + utbytte;
    }

    private void oppdaterAntall(double a) { antall += a; }

    public void leggTilUtbytte(double belop) { utbytte += belop; }

    // Metoden legger til et kjøp eller salg i listen med transaksjoner og oppdaterer antall og realisertAvkastning
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

    private void beregnGAV() {    
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
            gav = 0.0; // Setter GAV til 0 hvis antall er 0
        }
    }
}