import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.util.ArrayList;

public class Konto {
    private static ArrayList<Verdipapir> verdipapirer = new ArrayList<>();

    public static void main(String[] args) throws IOException {
        lesFil("transactions.csv");
        skrivTilCsv();
    }

    private static double parseDoubleOrZero(String value) {
        if (value == null || value.trim().isEmpty()) {
            return 0.0;
        }
        return Double.parseDouble(value.replaceAll(" ", ""));
    }

    private static String[] rengjorLinje(String linje) {
        return linje.replaceAll(" ", "")
                    .replaceAll("−", "-")
                    .replaceAll(",", ".")
                    .split(";");
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

    private static void opprettNyttVerdipapirHvisNodvendig(String verdipapirNavn, String isin) {
        if (!VerdipapirDuplicate(verdipapirNavn) && !verdipapirNavn.isEmpty()) {
            verdipapirer.add(new Verdipapir(verdipapirNavn, isin));
        }
    }

    private static class HeaderIndekser {
        int verdipapir, isin, transaksjonstype, belop, antall, kurs, resultat, totaleAvgifter;
        
        HeaderIndekser() {
            verdipapir = isin = transaksjonstype = belop = antall = kurs = resultat = totaleAvgifter = -1;
        }
    }

    private static HeaderIndekser finnHeaderIndekser(String header) {
        String[] headerDelt = header.split(";");
        HeaderIndekser indekser = new HeaderIndekser();
        
        for (int i = 0; i < headerDelt.length; i++) {
            switch (headerDelt[i]) {
                case "Verdipapir" -> indekser.verdipapir = i;
                case "ISIN" -> indekser.isin = i;
                case "Transaksjonstype" -> indekser.transaksjonstype = i;
                case "Beløp" -> indekser.belop = i;
                case "Antall" -> indekser.antall = i;
                case "Kurs" -> indekser.kurs = i;
                case "Resultat" -> indekser.resultat = i;
                case "Totale Avgifter" -> indekser.totaleAvgifter = i;
            }
        }
        return indekser;
    }

    private static void behandleLinje(String linje, HeaderIndekser indekser) {
        String[] linjeDelt = rengjorLinje(linje);
        
        String verdipapirNavn = linjeDelt[indekser.verdipapir];
        if (verdipapirNavn.isEmpty()) {
            return;
        }
        
        opprettNyttVerdipapirHvisNodvendig(verdipapirNavn, linjeDelt[indekser.isin]);
        
        Verdipapir verdipapir = hentVerdipapir(verdipapirNavn);
        if (verdipapir == null) {
            return;
        }
        
        behandleTransaksjon(verdipapir, linjeDelt, indekser);
    }

    private static void behandleTransaksjon(Verdipapir verdipapir, String[] linjeDelt, HeaderIndekser indekser) {
        String transaksjonstype = linjeDelt[indekser.transaksjonstype];
        double belop = parseDoubleOrZero(linjeDelt[indekser.belop]);
        double antall = parseDoubleOrZero(linjeDelt[indekser.antall]);
        double kurs = parseDoubleOrZero(linjeDelt[indekser.kurs]);
        double resultat = parseDoubleOrZero(linjeDelt[indekser.resultat]);
        double totaleAvgifter = parseDoubleOrZero(linjeDelt[indekser.totaleAvgifter]);
        
        switch (transaksjonstype) {
            case "SALG", "KJØPT", "REINVESTERTUTBYTTE" -> verdipapir.leggTilTransaksjon(belop, antall, kurs, resultat, totaleAvgifter);
            case "UTBYTTE" -> verdipapir.leggTilUtbytte(belop);
            case "INNSKUDD", "UTTAK INTERNET", "PLATTFORMAVGIFT",
                "TILBAKEBET. FOND AVG", "OVERBELÅNINGSRENTE", "TILBAKEBETALING" -> {
                // For å håndtere kontanter
            }
            //default -> System.out.println("Ikke behandlet transaksjonstype: " + transaksjonstype);
        }
    }

    private static void lesFil(String filnavn) throws IOException {
        try (Scanner leser = new Scanner(new File(filnavn))) {
            String header = leser.nextLine();
            HeaderIndekser indekser = finnHeaderIndekser(header);
            
            while (leser.hasNextLine()) {
                behandleLinje(leser.nextLine(), indekser);
            }
        } catch (FileNotFoundException e) {
            System.out.println(e.getMessage());
        }
    }

    private static void skrivTilCsv() {
        File file = new File("portfolio.csv");
        try (FileWriter writer = new FileWriter(file)) {
            skrivUtOversiktSomCsv(writer);
            new ProcessBuilder("open", "portfolio.csv").start();
        } catch (IOException e) {
            System.out.println("Feil ved skriving til fil: " + e.getMessage());
        }
    }

    private static void skrivUtOversiktSomCsv(FileWriter writer) throws IOException {
        writer.write("Ticker\tVerdipapir\tAntall\tGAV\tKurs\tKostpris\tMarkedsverdi\tUrealisert Avkastning (%)\tUrealisert Avkastning\tRealisert Avkastning (%)\tRealisert Avkastning\tUtbytte\tAvkastning (%)\tAvkastning\n");        
        int rad = 2;
        int startRad = rad;

        for (Verdipapir v : verdipapirer) {
            String ticker = v.hentTicker();
            String navn = v.hentNavn();
            
            String tickerCell = ticker.isEmpty() ? navn : "=HVISFEIL(AKSJE(\"" + ticker + "\";25);\"-\")";
            String navnCell = ticker.isEmpty() ? navn : "=HVISFEIL(AKSJE(A" + rad + ";1);\"" + navn + "\")";
            
            String kursFormel = "=HVISFEIL(AKSJE(A" + rad + ";0);0)";
            
            String kostprisFormel = "VERDI(C" + rad + ")*VERDI(D" + rad + ")";
            String markedsverdiFormel = "VERDI(C" + rad + ")*VERDI(E" + rad + ")";
            
            String urealisertAvkastningFormel = "=" + markedsverdiFormel + "-" + kostprisFormel;
            String urealisertAvkastningProsentFormel = "=AVRUND(HVISFEIL(((" + markedsverdiFormel + ")-(" + kostprisFormel + "))/(" + kostprisFormel + "); 0); 2)";
                        
            String totalAvkastningFormel = "I" + rad + "+K" + rad + "+L" + rad;
            String totalAvkastningProsentFormel = "=AVRUND(HVISFEIL((" + totalAvkastningFormel + ")/F" + rad + "; 0); 2)";

            writer.write(
                tickerCell + "\t" +
                navnCell + "\t" +
                v.hentAntall() + "\t" +
                v.hentGav() + "\t" +
                kursFormel + "\t" +
                "=" + kostprisFormel + "\t" +
                "=" + markedsverdiFormel + "\t" +
                urealisertAvkastningProsentFormel + "\t" +
                urealisertAvkastningFormel + "\t" +
                v.hentRealisertAvkastningProsent() + "\t" +
                v.hentRealisertAvkastning() + "\t" +
                v.hentUtbytte() + "\t" +
                totalAvkastningProsentFormel + "\t" +
                "=" + totalAvkastningFormel + "\n"
            );

            rad++;
        }

        writer.write("\t\t\t\t\t" +
            "=SUMMER(F" + startRad + ":F" + (rad-1) + ")\t" +
            "=SUMMER(G" + startRad + ":G" + (rad-1) + ")\t" +
            "=SUMMER(H" + startRad + ":H" + (rad-1) + ")\t" +
            "=SUMMER(I" + startRad + ":I" + (rad-1) + ")\t" +
            "=SUMMER(J" + startRad + ":J" + (rad-1) + ")\t" +
            "=SUMMER(K" + startRad + ":K" + (rad-1) + ")\t" +
            "=SUMMER(L" + startRad + ":L" + (rad-1) + ")\t" +
            "=SUMMER(M" + startRad + ":M" + (rad-1) + ")\t" +
            "=SUMMER(N" + startRad + ":N" + (rad-1) + ")\n"
        );
    }
}