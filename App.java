import com.sun.net.httpserver.HttpServer; // Webサーバーを使うために読み込みます。
import java.net.InetSocketAddress; // 待ち受ける番号を指定するために読み込みます。

public class App { // 実行するプログラムの名前を App にします。
    public static void main(String[] args) throws Exception { // ここからプログラムを始めます。
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0); // 8080番で待ち受けるサーバーを作ります。
        server.createContext("/", exchange -> { // 「/」へのアクセスが来たときの処理を書きます。
            System.out.println("ハンドラが動いた");
            String message = "こんにちは！！！"; // ブラウザに返す文字を決めます。
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8"); // 文字の種類を UTF-8 と伝えます。
            byte[] body = message.getBytes("UTF-8"); // 文字を送信用のデータに変えます。
            exchange.sendResponseHeaders(200, body.length); // 正常に返すこととデータの長さを伝えます。
            exchange.getResponseBody().write(body); // データをブラウザへ送ります。
            exchange.getResponseBody().close(); // 送信を終えます。
        }); // 「/」へのアクセスの処理を終えます。
        server.start(); // サーバーの待ち受けを始めます。
        System.out.println("サーバー起動: http://localhost:8080 （止めるときは Ctrl+C）"); // 起動したことを表示します。
    } // プログラムの処理を終えます。
} // App の定義を終えます。
