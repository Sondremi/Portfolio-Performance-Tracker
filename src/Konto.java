import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.util.ArrayList;

public class Konto {
    private static ArrayList<Verdipapir> verdipapirer = new ArrayList<>();
    private static double kontanterPaaKonto;
    private static double kontanterInvestert;
    private static double kontanterTilgjengelig;

    public static void main(String[] args) throws IOException {
        lesFil("src/transactions.csv"); // Endre til å bruke args
        skrivTilCsv();
        new ProcessBuilder("open", "src/oversikt.csv").start();
    }

    // Helper method to safely parse double values
    private static double parseDoubleOrZero(String value) {
        if (value == null || value.trim().isEmpty()) {
            return 0.0;
        }
        return Double.parseDouble(value.replaceAll(" ", ""));
    }

    private static void lesFil(String filnavn) throws IOException {
        try (Scanner leser = new Scanner(new File(filnavn))) {
            String header = leser.nextLine();
            String[] headerDelt = header.split(";");

            int verdipapirIndex = -1; int transaksjonstypeIndex = -1; int belopIndex = -1; int antallIndex = -1; int kursIndex = -1; int resultatIndex = -1; int totaleAvgifterIndex = -1;

            // Går gjennom header og henter index til de oppgitte typene
            for (int i = 0; i < headerDelt.length; i++) {
                if (headerDelt[i].contains("Verdipapir")) {
                    verdipapirIndex = i;
                } else if (headerDelt[i].contains("Transaksjonstype")) {
                    transaksjonstypeIndex = i;
                } else if (headerDelt[i].contains("Beløp")) {
                    belopIndex = i;
                } else if (headerDelt[i].contains("Antall")) {
                    antallIndex = i;
                } else if (headerDelt[i].contains("Kurs")) {
                    kursIndex = i;
                } else if (headerDelt[i].contains("Resultat")) {
                    resultatIndex = i;
                } else if (headerDelt[i].contains("Totale Avgifter")) {
                    totaleAvgifterIndex = i;
                }
            }

            while (leser.hasNextLine()) {
                String[] linjeDelt = leser.nextLine().replaceAll(" ", "").replaceAll("−", "-").replaceAll(",", ".").split(";");

                String verdipapirNavn = linjeDelt[verdipapirIndex];
                if (!VerdipapirDuplicate(verdipapirNavn) && !verdipapirNavn.equals("")) {
                    verdipapirer.add(new Verdipapir(verdipapirNavn));
                }

                if (verdipapirNavn.isEmpty()) {
                    continue;
                }

                Verdipapir verdipapir = hentVerdipapir(verdipapirNavn);
                if (verdipapir == null) {
                    continue;
                }

                String transaksjonstype = linjeDelt[transaksjonstypeIndex];
                double belop = parseDoubleOrZero(linjeDelt[belopIndex]);
                double antall = parseDoubleOrZero(linjeDelt[antallIndex]);
                double kurs = parseDoubleOrZero(linjeDelt[kursIndex]);
                double resultat = parseDoubleOrZero(linjeDelt[resultatIndex]);
                double totaleAvgifter = parseDoubleOrZero(linjeDelt[totaleAvgifterIndex]);

                if (transaksjonstype.equals("SALG") || transaksjonstype.equals("KJØPT") || transaksjonstype.equals("REINVESTERT UTBYTTE")) {
                    verdipapir.leggTilTransaksjon(belop, antall, kurs, resultat, totaleAvgifter);
                    if (transaksjonstype.equals("REINVESTERT UTBYTTE")) {
                        verdipapir.leggTilUtbytte(belop); // Setter utbyttet som kostpris, istedet for utbetalt utbytte på konto
                    }
                } else if (transaksjonstype.equals("UTBYTTE")) {
                    verdipapir.leggTilUtbytte(belop);
                    leggTilKontanter(belop);
                } else if (transaksjonstype.equals("INNSKUDD") || transaksjonstype.equals("UTTAK INTERNET") || transaksjonstype.equals("PLATTFORMAVGIFT")
                        || transaksjonstype.equals("TILBAKEBET. FOND AVG") || transaksjonstype.equals("OVERBELÅNINGSRENTE") || transaksjonstype.equals("TILBAKEBETALING")) {
                    leggTilKontanter(belop);
                } 
                // Sjekker om en transaksjonstype ikke er behandlet
                else {
                    System.out.println("Ikke behandlet transaksjonstype: " + transaksjonstype);
                } 

                // Oppdaterer verdipapirets data fortløpende
                if (verdipapir != null) {
                    verdipapir.beregnData();
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println(e.getMessage());
        }
    }

    private static void skrivTilCsv() {
        File file = new File("src/oversikt.csv");
        try (FileWriter writer = new FileWriter(file)) {
            skrivUtOversiktSomCsv(writer);
            System.out.println("Oversikt skrevet til filen 'oversikt.csv'.");
        } catch (IOException e) {
            System.out.println("Feil ved skriving til fil: " + e.getMessage());
        }
    }

    private static void skrivUtOversiktSomCsv(FileWriter writer) throws IOException {
        writer.write("Ticker\tVerdipapir\tAntall\tGAV\tKurs\tKostpris\tMarkedsverdi\tUrealisert Avkastning (%)\tUrealisert Avkastning\tRealisert Avkastning (%)\tRealisert Avkastning\tUtbytte\tAvkastning (%)\tAvkastning\n");        
        int rad = 2;

        for (Verdipapir v : verdipapirer) {
            String ticker = v.hentTicker();

            String aksjeNavnFormel = "=HVISFEIL(AKSJE(\"" + ticker + "\";25);\"-\")";
            String antall = String.valueOf(v.hentAntall()).replace('.', ',');
            String gav = String.valueOf(v.hentGav()).replace('.', ',');
            String aksjeKursFormel = "=HVISFEIL(AKSJE(A" + rad + ";0);\"-\")";
            String kostprisFormel = "VERDI(C" + rad + ")*VERDI(D" + rad + ")";
            String markedsverdiFormel = "VERDI(C" + rad + ")*VERDI(E" + rad + ")";
            String urealisertAvkastningFormel = markedsverdiFormel + "-" + kostprisFormel;
            String urealisertProsentFormel = "HVISFEIL((" + urealisertAvkastningFormel + ")/" + kostprisFormel + ";0)";

            writer.write(
                aksjeNavnFormel + "\t" +
                v.hentNavn() + "\t" +
                antall + "\t" +
                gav + "\t" +
                aksjeKursFormel + "\t" +
                "=" + kostprisFormel + "\t" +
                "=" + markedsverdiFormel + "\t" +
                "=" + urealisertProsentFormel + "\t" +
                "=" + urealisertAvkastningFormel + "\t" +
                v.hentRealisertAvkastningProsent() + "\t" +
                v.hentRealisertAvkastning() + "\t" +
                v.hentUtbytte() + "\t" +
                v.hentTotalAvkastningProsent() + "\t" +
                v.hentTotalAvkastning() + "\n"
            );

            rad++;
        }
    }

    private static boolean VerdipapirDuplicate(String navn) {
        for (Verdipapir v : verdipapirer) {
            if (v.hentNavn().equals(navn)) {
                return true;
            }
        }
        return false;
    }

    private static Verdipapir hentVerdipapir(String navn) {
        for (Verdipapir v : verdipapirer) {
            if (v.hentNavn().equals(navn)) {
                return v;
            }
        }
        return null;
    }

    private static void leggTilKontanter(double belop) {kontanterPaaKonto += belop; oppdaterKontanter();}

    private static void oppdaterKontanter() {  
        kontanterInvestert = 0.0;
        double realisertAvkastning = 0.0;     
        for (Verdipapir v : verdipapirer) {     
            kontanterInvestert += v.hentKostpris();
            realisertAvkastning += v.hentRealisertAvkastning();
        }
        kontanterTilgjengelig = Double.parseDouble(String.format("%.2f", kontanterPaaKonto - kontanterInvestert + realisertAvkastning).replace(",", "."));
    }
}