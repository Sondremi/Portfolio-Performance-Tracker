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
        lesFil("src/transactions.csv"); // Change to your own file
        skrivTilFil();
    }

    private static void lesFil(String filnavn) throws IOException {
        try (Scanner leser = new Scanner(new File(filnavn))) {
            // Header : lagrer index til riktig typer fra fil
            String header = leser.nextLine();
            String[] headerDelt = header.split(";");

            int navnIndex = -1; int transaksjonstypeIndex = -1; int belopIndex = -1; int antallIndex = -1; int kursIndex = -1; int resultatIndex = -1; int totaleAvgifterIndex = -1;

            // Går gjennom header og henter index til de oppgitte typene
            for (int i = 0; i < headerDelt.length; i++) {
                if (headerDelt[i].contains("Verdipapir")) {
                    navnIndex = i;
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

                String verdipapirNavn = linjeDelt[navnIndex];
                if (!VerdipapirDuplicate(verdipapirNavn) && !verdipapirNavn.equals("")) {
                    verdipapirer.add(new Verdipapir(verdipapirNavn));
                }

                Verdipapir verdipapir = hentVerdipapir(verdipapirNavn); // Bruker navnIndex
                String transaksjonstype = linjeDelt[transaksjonstypeIndex]; // Bruker transaksjonstypeIndex
                double belop = Double.parseDouble(linjeDelt[belopIndex].replaceAll(" ", "")); // Bruker belopIndex
                double antall = Double.parseDouble(linjeDelt[antallIndex].replaceAll(" ", "")); // Bruker antallIndex
                double kurs = Double.parseDouble(linjeDelt[kursIndex].replaceAll(" ", "")); // Bruker kursIndex
                double resultat = Double.parseDouble(linjeDelt[resultatIndex].replaceAll(" ", "")); // Bruker resultatIndex
                double totaleAvgifter = Double.parseDouble(linjeDelt[totaleAvgifterIndex].replaceAll(" ", "")); // Bruker totaleAvgifterIndex

                if (transaksjonstype.equals("SALG") || transaksjonstype.equals("KJØPT") || transaksjonstype.equals("REINVESTERT UTBYTTE")) {
                    verdipapir.leggTilTransaksjon(belop, antall, kurs, resultat, totaleAvgifter);
                    if (transaksjonstype.equals("REINVESTERT UTBYTTE")) {
                        verdipapir.leggTilUtbytte(belop); // Setter utbyttet som kostpris, istedet for utbytte
                    }

                } else if (transaksjonstype.equals("UTBYTTE")) {
                    verdipapir.leggTilUtbytte(belop);
                    leggTilKontanter(belop);

                } else if (transaksjonstype.equals("INNSKUDD") || transaksjonstype.equals("UTTAK INTERNET") || transaksjonstype.equals("PLATTFORMAVGIFT")
                        || transaksjonstype.equals("TILBAKEBET. FOND AVG") || transaksjonstype.equals("OVERBELÅNINGSRENTE") || transaksjonstype.equals("TILBAKEBETALING")) {
                    leggTilKontanter(belop);
                } 
                // Sjekker om en transaksjonstype ikke er behandlet
                /* else {
                    System.out.println("Ikke behandlet transaksjonstype: " + transaksjonstype);
                }  */

                // Oppdaterer verdipapirets data fortløpende
                if (verdipapir != null) {
                    verdipapir.beregnData();
                }
            }
            sorterVerdipapirListe(); // Sorterer listen basert på valget i metoden
        } catch (FileNotFoundException e) {
            System.out.println(e.getMessage());
        }
    }

    private static void skrivTilFil() {
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
            String ticker = v.hentNavn();
            String antall = "" + v.hentBeholdning();
            String gav = "" + v.hentGav();
            String kurs = "" + v.hentKurs();
            String kostpris = "" + v.hentKostpris();
            String markedsverdi = "" + v.hentMarkedsverdi();
            String urealisertGevinst = "" + v.hentUrealisertAvkastning();
            String realisertGevinst = "" + v.hentRealisertAvkastning();
            String utbytte = "" + v.hentUtbytte();
            String totalAvkastning = "" + v.hentTotalAvkastning();
            writer.write(String.format("| %-40s | %-8s | %-8s | %-8s | %-12s | %-12s | %-18s | %-18s | %-8s | %-16s |\n", ticker, antall, gav, kurs, kostpris, markedsverdi, urealisertGevinst, realisertGevinst, utbytte, totalAvkastning));
        }
        
        writer.write(String.format("|%s|\n", "-".repeat(dashes)));
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
            kontanterInvestert += v.hentKostpris(); // Legger til kostprisen
            realisertAvkastning += v.hentRealisertAvkastning();
        }
        kontanterTilgjengelig = Double.parseDouble(String.format("%.2f",kontanterPaaKonto - kontanterInvestert + realisertAvkastning).replace(",", "."));
    }
    
    private static void sorterVerdipapirListe() {
        // Endre sorteringsKriterium etter hvordan du ønsker sorteringen:
        // 1: Størst beholdning
        // 2: Realisert avkastning
        // 3: Urealisert avkastning
        // 4: Total avkastning
        int sorteringsKriterium = 4;

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
                        if (verdipapir1.hentBeholdning() < verdipapir2.hentBeholdning()) {
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
                        if (verdipapir1.hentBeholdning() < verdipapir2.hentBeholdning()) {
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