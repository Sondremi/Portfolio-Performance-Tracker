import java.util.ArrayList;
import java.util.Arrays;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Verdipapir {
    private ArrayList<ArrayList<Object>> transaksjoner = new ArrayList<>();
    private final String navn;
    private String ticker = "";
    private String isin = "";
    private Double gav = 0.0;
    private Double antall = 0.0;
    private Double utbytte = 0.0;
    private Double realisertAvkastning = 0.0;

    public Verdipapir(String n, String i) {
        navn = n;
        isin = i;
        settTicker();
    }

    public String hentNavn() { return navn; }
    public String hentTicker() { return ticker; }
    public String hentGav() { return String.format("%.2f", gav); }
    public String hentAntall() { return String.format("%.2f", antall); }
    public String hentUtbytte() { return String.format("%.2f", utbytte); }
    public String hentRealisertAvkastning() { return String.format("%.2f", realisertAvkastning); }

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
    
    private void settTicker() {
        if (isin == null || isin.isEmpty()) {
            ticker = "";
            return;
        }

        try {
            String url = "https://query2.finance.yahoo.com/v1/finance/search?q=" + 
                        URLEncoder.encode(isin, "UTF-8") + "&quotesCount=1";
            
            String response = httpGetRequest(url);
            
            String symbol = extractValue(response, "symbol");
            String exchange = extractValue(response, "exchange");
            String quoteType = extractValue(response, "quoteType");
            
            if (symbol != null && !symbol.isEmpty()) {
                if ("ETF".equals(quoteType) || "MUTUALFUND".equals(quoteType)) {
                    ticker = symbol;
                } 
                else if (exchange != null && !exchange.isEmpty()) {
                    String exchangeSuffix = getExchangeSuffix(exchange);
                    ticker = symbol + (exchangeSuffix.isEmpty() ? "" : "." + exchangeSuffix);
                } else {
                    ticker = symbol;
                }
            } else {
                ticker = "";
            }
        } catch (Exception e) {
            System.err.println("Feil ved Yahoo Finance ISIN-søk: " + e.getMessage());
            ticker = "";
        }
    }

    private String getExchangeSuffix(String exchangeName) {
        return switch (exchangeName.toLowerCase()) {
            case "oslo" -> "OL";
            case "new york stock exchange", "nyse" -> "NYSE";
            case "nasdaq" -> "";
            case "london" -> "L";
            case "xetra", "frankfurt" -> "DE";
            case "paris" -> "PA";
            case "tokyo" -> "T";
            case "hong kong" -> "HK";
            case "sydney" -> "AX";
            case "toronto" -> "TO";
            default -> "";
        };
    }

    private String extractValue(String json, String key) {
        Pattern pattern = Pattern.compile("\"" + key + "\":\"?(.*?)(\"|,|})");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            String value = matcher.group(1);
            return value.equals("null") ? null : value;
        }
        return null;
    }

    private String httpGetRequest(String urlString) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlString).openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", "Mozilla/5.0");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        
        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }
        return response.toString();
    }
}