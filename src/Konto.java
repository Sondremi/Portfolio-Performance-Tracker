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

    // Endre sorteringsKriterium etter hvordan du ønsker sorteringen:
    // 1: Størst beholdning
    // 2: Realisert avkastning
    // 3: Urealisert avkastning
    // 4: Total avkastning
    private static int sorteringsKriterium = 4;

    public static void main(String[] args) throws IOException {
        lesFil("src/transactions.csv"); // Endre til å bruke args
        //skrivTilFil();
        skrivTilCsv();
        Runtime.getRuntime().exec("open src/oversikt.csv");
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

            sorterVerdipapirListe();
        } catch (FileNotFoundException e) {
            System.out.println(e.getMessage());
        }
    }

    // Metode for å skrive ut oversikten til en tekstfil
    /* private static void skrivTilFil() {
        File file = new File("src/oversikt.txt");
        try (FileWriter writer = new FileWriter(file)) {
            skrivUtOversikt(writer);
            System.out.println("Oversikt skrevet til filen 'oversikt.txt'.");
        } catch (IOException e) {
            System.out.println("Feil ved skriving til fil: " + e.getMessage());
        }
    }
    private static void skrivUtOversikt(FileWriter writer) throws IOException {
        int dashes = 177;
        writer.write(String.format("|%s|\n", "-".repeat(dashes)));
        writer.write(String.format("| %-175s |\n", "Kontanter investert: " + kontanterInvestert));
        writer.write(String.format("| %-175s |\n", "Kontanter tilgjengelig: " + kontanterTilgjengelig));
        writer.write(String.format("|%s|\n", "-".repeat(dashes)));

        writer.write(String.format("| %-40s | %-8s | %-8s | %-8s | %-12s | %-12s | %-18s | %-18s | %-8s | %-16s |\n", "Ticker", "Antall", "GAV", "Kurs", "Kostpris", "Markedsverdi", "Urealisert gevinst", "Realisert gevinst", "Utbytte", "Total avkastning"));
        writer.write(String.format("| %-1s | %-1s | %-1s | %-1s | %-1s | %-1s | %-1s | %-1s | %-1s | %-1s |\n", "----------------------------------------", "--------", "--------", "--------", "------------", "------------", "------------------", "------------------", "--------", "----------------"));

        for (Verdipapir v : verdipapirer) {
            writer.write(String.format("| %-40s | %-8s | %-8s | %-8s | %-12s | %-12s | %-18s | %-18s | %-8s | %-16s |\n", 
            v.hentNavn(), v.hentAntall(), v.hentGav(), v.hentKurs(), v.hentKostpris(), v.hentMarkedsverdi(), v.hentUrealisertAvkastning(), v.hentRealisertAvkastning(), v.hentUtbytte(), v.hentTotalAvkastning()));
        }
        
        writer.write(String.format("|%s|\n", "-".repeat(dashes)));
    } */

    // Metode for å skrive ut oversikten som et numbers ark
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
            String urealisertGevinstFormel = markedsverdiFormel + "-" + kostprisFormel;
            String urealisertProsentFormel = "HVISFEIL((" + urealisertGevinstFormel + ")/" + kostprisFormel + ";0)";

            writer.write(
                aksjeNavnFormel + "\t" +                    // A: AKSJE-funksjon for navn
                v.hentNavn() + "\t" +                       // B: Navn
                antall + "\t" +                             // C: Antall
                gav + "\t" +                                // D: GAV
                aksjeKursFormel + "\t" +                    // E: Kurs (AKSJE)
                "=" + kostprisFormel + "\t" +               // F: Kostpris
                "=" + markedsverdiFormel + "\t" +           // G: Markedsverdi
                "=" + urealisertProsentFormel + "\t" +      // H: Urealisert avkastning i %
                "=" + urealisertGevinstFormel + "\t" +      // I: Urealisert gevinst
                "" + "\t" +                                 // J: Realisert % gevinst
                v.hentRealisertAvkastning() + "\t" +        // K: Realisert gevinst
                v.hentUtbytte() + "\t" +                    // L: Utbytte
                "" + "\t" +                                 // M: Total %
                v.hentTotalAvkastning() + "\n"              // N: Total gevinst
            );

            rad++;
        }

        // Summer 
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
    
    private static void sorterVerdipapirListe() {
        // Opprett en ny liste med verdipapirene for sortering
        ArrayList<Verdipapir> sortertVerdipapirer = new ArrayList<>(verdipapirer);
    
        // Sorteringsalgoritme (Bubble Sort)
        boolean erSortert = false;
        while (!erSortert) {
            erSortert = true; // Anta at listen er sortert
    
            for (int i = 0; i < sortertVerdipapirer.size() - 1; i++) {
                Verdipapir verdipapir1 = sortertVerdipapirer.get(i);
                Verdipapir verdipapir2 = sortertVerdipapirer.get(i + 1);
    
                // Sammenlign basert på sorteringskriterium
                switch (sorteringsKriterium) {
                    case 1: // Sorter på antall
                        if (verdipapir1.hentAntall() < verdipapir2.hentAntall()) {
                            // Bytt plass hvis beholdning er mindre
                            sortertVerdipapirer.set(i, verdipapir2);
                            sortertVerdipapirer.set(i + 1, verdipapir1);

                            // Marker at listen ikke er fullstendig sortert
                            erSortert = false;
                        }
                        break;
                    case 2: // Sorter på realisert avkastning
                        if (verdipapir1.hentRealisertAvkastning() < verdipapir2.hentRealisertAvkastning()) {
                            // Bytt plass hvis beholdning er mindre
                            sortertVerdipapirer.set(i, verdipapir2);
                            sortertVerdipapirer.set(i + 1, verdipapir1);

                            // Marker at listen ikke er fullstendig sortert
                            erSortert = false;
                        }
                        break;
                    case 3: // Sorter på urealisert avkastning
                        if (verdipapir1.hentUrealisertAvkastning() < verdipapir2.hentUrealisertAvkastning()) {
                            // Bytt plass hvis beholdning er mindre
                            sortertVerdipapirer.set(i, verdipapir2);
                            sortertVerdipapirer.set(i + 1, verdipapir1);

                            // Marker at listen ikke er fullstendig sortert
                            erSortert = false;
                        }
                        break;
                    case 4: // Sorter på total avkastning
                        if (verdipapir1.hentTotalAvkastning() < verdipapir2.hentTotalAvkastning()) {
                            // Bytt plass hvis beholdning er mindre
                            sortertVerdipapirer.set(i, verdipapir2);
                            sortertVerdipapirer.set(i + 1, verdipapir1);

                            // Marker at listen ikke er fullstendig sortert
                            erSortert = false;
                        }
                        break;
                    default:
                        System.out.println("Ugyldig sorteringskriterium. Sorterer på antall.");
                        if (verdipapir1.hentAntall() < verdipapir2.hentAntall()) {
                            // Bytt plass hvis beholdning er mindre
                            sortertVerdipapirer.set(i, verdipapir2);
                            sortertVerdipapirer.set(i + 1, verdipapir1);

                            // Marker at listen ikke er fullstendig sortert
                            erSortert = false;
                        }
                        break;
                }
            }
        }
        // Oppdater den opprinnelige listen med den sorterte listen
        verdipapirer = sortertVerdipapirer;
    }
}