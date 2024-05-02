import java.util.Arrays;
import java.util.ArrayList;

public class Verdipapir {
    private ArrayList<ArrayList<Object>> transaksjoner = new ArrayList<>();
    private final String navn;
    private Double gav = 0.0;
    private Double kurs = 0.0;
    private Double utbytte = 0.0;
    private Double kostpris = 0.0;
    private Double beholdning = 0.0;
    private Double markedsverdi = 0.0;
    private Double totalAvkastning = 0.0;
    private Double realisertAvkastning = 0.0;
    private Double urealisertAvkastning = 0.0;

    public Verdipapir(String n) {
        navn = n;
    }

    // Metoder for å hente verdier fra verdipapiret
    public String hentNavn() {return navn;}
    public ArrayList<ArrayList<Object>> hentTransaksjoner() {return transaksjoner;}
    public double hentGav() {return Double.parseDouble(String.format("%.2f", gav).replace(",", "."));}
    public double hentKurs() {return Double.parseDouble(String.format("%.2f", kurs).replace(",", "."));}
    public double hentUtbytte() {return Double.parseDouble(String.format("%.2f", utbytte).replace(",", "."));}
    public double hentKostpris() {return Double.parseDouble(String.format("%.2f", kostpris).replace(",", "."));}
    public double hentBeholdning() {return Double.parseDouble(String.format("%.2f", beholdning).replace(",", "."));}
    public double hentMarkedsverdi() {return Double.parseDouble(String.format("%.2f", markedsverdi).replace(",", "."));}
    public double hentTotalAvkastning() {return Double.parseDouble(String.format("%.2f", totalAvkastning).replace(",", "."));}
    public double hentRealisertAvkastning() {return Double.parseDouble(String.format("%.2f", realisertAvkastning).replace(",", "."));}
    public double hentUrealisertAvkastning() {return Double.parseDouble(String.format("%.2f", urealisertAvkastning).replace(",", "."));}

    // Metoden beregner all dataen til verdipapiret
    public void beregnData() {
        oppdaterKurs();
        beregnGAV();
        kurs = gav * 1.25; // Slett når API er implementert
        kostpris = gav * beholdning;
        markedsverdi = kurs * beholdning;
        urealisertAvkastning = markedsverdi - kostpris;
        totalAvkastning = urealisertAvkastning + realisertAvkastning + utbytte;
    }

    // Metoden oppdaterer antall verdipapirer
    private void oppdaterBeholdning(double antall) {beholdning += antall;}

    // Metoden legger til utbytte til verdipapiret
    public void leggTilUtbytte(double belop) {utbytte+=belop;}

    // Metoden legger til et kjøp eller salg i listen med transaksjoner og oppdaterer beholding og realisertAvkastning
    public void leggTilTransaksjon(double verdi, double antall, double kurs, double resultat, double totaleAvgifter) {
        if (verdi < 0) {
            transaksjoner.add(new ArrayList<>(Arrays.asList("KJØPT", ((antall * kurs) + totaleAvgifter))));
            oppdaterBeholdning(antall);
        } else {
            transaksjoner.add(new ArrayList<>(Arrays.asList("SALG", ((antall * kurs) + totaleAvgifter))));
            oppdaterBeholdning(-antall);
            realisertAvkastning += resultat;
        }
    }

    // Metoden beregner verdipapirets GAV
    private void beregnGAV() {    
        double gav_tmp = 0.0;
        // Iterer over alle transaksjoner
        for (ArrayList<Object> trans : transaksjoner) {
            double belop = (double) trans.get(1);

            if (trans.get(0).equals("KJØPT")) {
                gav_tmp += belop;
            } else {
                gav_tmp -= belop;
            } 
        }
    
        // Deler gjennomsnittlig kostnad med beholdning
        if (beholdning > 0) {
            gav =  gav_tmp / beholdning;
        } else {
            gav = 0.0; // Setter GAV til 0 hvis beholdning er 0
        }
    }


    public void oppdaterKurs() {    
        // implementer egen API for å hente kurser
    }
    
}