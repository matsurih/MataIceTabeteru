package io.github.matsurihime.mataicetabeteru;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import net.arnx.jsonic.JSON;

public class YahooAPI {

    /**
     * Yahoo!ディベロッパーのAPP ID
     */
    private static String APP_ID = "dj0zaiZpPXdGTVlia2ozcnByWSZzPWNvbnN1bWVyc2VjcmV0Jng9YTc-";

    /**
     * Yahoo!ショッピングAPIのベースURI
     */
    private static String BASE_URI = "http://shopping.yahooapis.jp/ShoppingWebService/V1/json/itemSearch";

    public static Map<String, String> requestYahooAPI(String code) {
        Map<String, String> ret = new HashMap<>();
        try {
            Product product = new Product();
            product.janCode = Long.parseLong(code);

            System.out.println(JSON.encode(product));

            YahooAPI.productData(product);

            System.out.println(JSON.encode(product));

            ret.put("jan", code);
            ret.put("name", product.name);
            ret.put("uri", product.uri);

        } catch (IOException e) {
            e.printStackTrace();
        }
        return ret;
    }

    /**
     * YahooショッピングAPIを使って商品データを検索し、Productインスタンスの該当フィールドを補足する
     *
     * @param product
     * @throws IOException
     */
    public static void productData(Product product) throws IOException {

        if (product != null && product.janCode != null) {

            URL url = new URL(BASE_URI + "?appid=" + APP_ID + "&jan=" + product.janCode);

            HttpURLConnection urlconn = (HttpURLConnection) url.openConnection();
            urlconn.setRequestMethod("GET");
            urlconn.setInstanceFollowRedirects(false);

            urlconn.connect();

            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(urlconn.getInputStream()));

            StringBuffer responseBuffer = new StringBuffer();
            while (true) {
                String line = reader.readLine();
                if (line == null) {
                    break;
                }
                responseBuffer.append(line);
            }
            reader.close();
            urlconn.disconnect();
            String response = responseBuffer.toString();
            parse(product, response);
        }
    }

    /**
     * JSONテキストをパースして、Productインスタンスの該当フィールドに追加
     *
     * @param product
     * @param jsonText
     */
    private static void parse(Product product, String jsonText) {

        Map<String, Map<String, Object>> json = JSON.decode(jsonText);

        if (!Integer.valueOf((String) json.get("ResultSet").get("totalResultsAvailable")).equals(0)) {

            @SuppressWarnings("unchecked")
            Map<String, Object> result = ((Map<String, Object>) (
                    (Map<String, Map<String, Object>>) json.get("ResultSet").get("0")
            ).get("Result").get("0")
            );

            String name = result.get("Name").toString();
            @SuppressWarnings("unchecked")
            String imageUrl = ((Map<String, Object>) result.get("Image")).get("Medium").toString();

            product.name = name;
            product.uri = imageUrl;

        }

    }

}

/**
 * 商品データを受け取るクラス
 */
class Product {
    public Long janCode;
    public String name;
    public String uri;
}