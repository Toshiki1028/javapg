import com.sun.net.httpserver.HttpServer; // Webサーバーを使うために読み込みます。
import java.net.InetSocketAddress; // 待ち受ける番号を指定するために読み込みます。
import java.net.URLDecoder; // URL用に変換された文字を元に戻すために読み込みます。
import java.nio.charset.StandardCharsets; // UTF-8を指定するために読み込みます。
import java.util.ArrayList; // Todoを入れるリストを作るために読み込みます。
import java.util.List; // Todoを入れるリストの型を使うために読み込みます。

public class App { // 実行するプログラムの名前を App にします。
    public static void main(String[] args) throws Exception { // ここからプログラムを始めます。
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0); // 8080番で待ち受けるサーバーを作ります。
        // 追加されたTodoを保存する空のリストを一度だけ作ります。
        List<String> todos = new ArrayList<>(); // Todoを入れるリストを作ります。
        server.createContext("/", exchange -> { // 「/」へのアクセスが来たときの処理を書きます。
            String path = exchange.getRequestURI().getPath(); // アクセスされたパスを取り出します。
            String method = exchange.getRequestMethod(); // GETかPOSTかを取り出します。
            String message;
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8"); // 通常の応答はUTF-8の文字として返します。
            if (path.equals("/add") && method.equals("POST")) { // フォームから送られたTodoを受け取ります。
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8); // 送られた内容をUTF-8の文字として読みます。
                String value = body.substring(5); // 先頭の「todo=」を除きます。
                String todo = URLDecoder.decode(value, StandardCharsets.UTF_8); // 日本語などの文字を元に戻します。
                if (!todo.isEmpty()) { // 空の入力は追加しません。
                    todos.add(todo); // 入力されたTodoをリストに追加します。
                } // 空の入力かどうかの確認を終えます。
                exchange.getResponseHeaders().set("Location", "/"); // 戻り先を「/」に指定します。
                exchange.sendResponseHeaders(303, -1); // ブラウザに戻り先へ移動するよう伝えます。
                exchange.close(); // この応答を閉じます。
                return; // ここで追加の処理を終えます。
            } else if (path.equals("/")) { // 「/」で入力フォームとTodoの一覧を表示します。
                String html = "<form method='post' action='/add'><input name='todo'><button>追加</button></form><ul>";
                for (String todo : todos) { // Todoを1件ずつ取り出します。
                    html += "<li>" + todo + "</li>"; // 取り出したTodoをHTMLに足します。
                } // すべてのTodoを足したので繰り返しを終えます。
                html += "</ul>"; // 箇条書きのHTMLを閉じます。
                message = html; // 作ったHTMLを応答に使います。
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8"); // 「/」はHTMLとして返します。
            } else if (path.equals("/hello")) { // パスが「/hello」か比べます。
                String query = exchange.getRequestURI().getRawQuery(); // URLのクエリを取り出します。
                String name = query == null ? "ゲスト" : query.substring(5); // nameがなければ「ゲスト」、あれば「name=」の後ろを切り出します。
                name = URLDecoder.decode(name, StandardCharsets.UTF_8); // %で始まる表記を日本語などの文字に戻します。
                message = "こんにちは、" + name + "さん！"; // 名前を応答の文に入れます。
                System.out.println("query = " + query);
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
