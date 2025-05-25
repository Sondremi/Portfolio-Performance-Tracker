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

    // Konverter navn til ticker
    private void settTicker() {
        // Navn kan være "Aker BP", "DNB ASA", KOAo, etc for aksjer.
        // For fond kan det være "DNB Global Indeks A", "Heimdal Høyrente N", Nordnet Global Indeks 125 NOK, etc.
        ticker = "HAUTO.OL"; // Dummy
    }

    private void beregnGAV() {    
        if (antall == 0) {
            gav = 0.0;
            return;
        }

        double total = transaksjoner.stream()
            .mapToDouble(trans -> {
                boolean erKjop = "KJØP".equals(trans.get(0));
                double belop = (double) trans.get(1);
                return erKjop ? belop : -belop;
            })
            .sum();
        
        gav = total / antall;
    }

    public String hentNavn() { return navn; }
    public String hentTicker() { return ticker; }
    public String hentGav() { return String.format("%.2f", gav); }
    public String hentAntall() { return String.format("%.2f", antall); }
    public String hentUtbytte() { return String.format("%.2f", utbytte); }
    public String hentRealisertAvkastning() { return String.format("%.2f", realisertAvkastning); }

    public void leggTilUtbytte(double belop) { utbytte += belop; }

    public void leggTilTransaksjon(double verdi, double oppdatertAntall, double kurs, double resultat, double totaleAvgifter) {
        String transaksjonsType = (verdi < 0) ? "KJØP" : "SALG";
        double totalBelop = oppdatertAntall * kurs + totaleAvgifter;
        
        transaksjoner.add(new ArrayList<>(Arrays.asList(transaksjonsType, totalBelop)));
        
        if (transaksjonsType.equals("KJØP")) {
            antall += oppdatertAntall;
        } else {
            antall -= oppdatertAntall;
            realisertAvkastning += resultat;
        }
        
        beregnGAV();
    }
}