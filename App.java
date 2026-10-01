import com.sun.net.httpserver.HttpServer; // Webサーバーを使うために読み込みます。
import java.net.InetSocketAddress; // 待ち受ける番号を指定するために読み込みます。
import java.net.URLDecoder; // URL用に変換された文字を元に戻すために読み込みます。
import java.nio.charset.StandardCharsets; // UTF-8を指定するために読み込みます。
import java.util.ArrayList; // Todoを入れるリストを作るために読み込みます。
import java.util.List; // Todoを入れるリストの型を使うために読み込みます。

public class App { // 実行するプログラムの名前を App にします。
    public static void main(String[] args) throws Exception { // ここからプログラムを始めます。
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0); // 8080番で待ち受けるサーバーを作ります。
        server.createContext("/", exchange -> { // 「/」へのアクセスが来たときの処理を書きます。
            String path = exchange.getRequestURI().getPath(); // アクセスされたパスを取り出します。
            String message;
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8"); // 通常の応答はUTF-8の文字として返します。
            if (path.equals("/hello")) { // パスが「/hello」か比べます。
                String query = exchange.getRequestURI().getRawQuery(); // URLのクエリを取り出します。
                String name = query == null ? "ゲスト" : query.substring(5); // nameがなければ「ゲスト」、あれば「name=」の後ろを切り出します。
                name = URLDecoder.decode(name, StandardCharsets.UTF_8); // %で始まる表記を日本語などの文字に戻します。
                message = "こんにちは、" + name + "さん！"; // 名前を応答の文に入れます。
                System.out.println("query = " + query);
            } else if (path.equals("/todos")) { // パスが「/todos」か比べます。
                List<String> todos = new ArrayList<>(); // Todoの文字列を入れるリストを作ります。
                todos.add("牛乳を買う"); // 1件目のTodoを入れます。
                todos.add("卵を買う"); // 2件目のTodoを入れます。
                todos.add("パンを買う"); // 3件目のTodoを入れます。
                todos.add("街に出る");
                String html = "<ul>"; // 箇条書きのHTMLを始めます。
                for (String todo : todos) { // Todoを1件ずつ取り出します。
                    html += "<li>" + todo + "</li>"; // 取り出したTodoをHTMLに足します。
                } // すべてのTodoを足したので繰り返しを終えます。
                html += "</ul>"; // 箇条書きのHTMLを閉じます。
                message = html; // 作ったHTMLを応答に使います。
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8"); // このパスだけHTMLとして返します。
            } else if (path.equals("/bye")) { // パスが「/bye」か比べます。
                message = "さようなら！";
            } else if (path.equals("/hobby")) { // パスが「/bye」か比べます。
                message = "酒！";
            } else {
                message = "ページが見つかりません";
            }
            byte[] body = message.getBytes("UTF-8"); // 文字を送信用のデータに変えます。
            exchange.sendResponseHeaders(200, body.length); // 正常に返すこととデータの長さを伝えます。
            exchange.getResponseBody().write(body); // データをブラウザへ送ります。
            exchange.getResponseBody().close(); // 送信を終えます。
        }); // 「/」へのアクセスの処理を終えます。
        server.start(); // サーバーの待ち受けを始めます。
        System.out.println("サーバー起動: http://localhost:8080 （止めるときは Ctrl+C）"); // 起動したことを表示します。
    } // プログラムの処理を終えます。
} // App の定義を終えます。
