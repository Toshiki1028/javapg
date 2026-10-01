import com.sun.net.httpserver.HttpServer; // Webサーバーを使うために読み込みます。
import java.net.InetSocketAddress; // 待ち受ける番号を指定するために読み込みます。
import java.net.URLDecoder; // URL用に変換された文字を元に戻すために読み込みます。
import java.nio.charset.StandardCharsets; // UTF-8を指定するために読み込みます。
import java.util.ArrayList; // Todoを入れるリストを作るために読み込みます。
import java.util.List; // Todoを入れるリストの型を使うために読み込みます。

public class App { // 実行するプログラムの名前を App にします。
    static List<Todo> todos = new ArrayList<>(); // ★変更 Todoを保存するリストです。
    static int nextId = 1; // ★変更 次に振る番号です。

    public static void main(String[] args) throws Exception { // ここからプログラムを始めます。
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0); // 8080番で待ち受けるサーバーを作ります。
        todos.add(new Todo(nextId++, "牛乳を買う")); // ★変更 1件目のサンプルを追加します。
        Todo egg = new Todo(nextId++, "卵を買う"); // ★変更 2件目のサンプルを作ります。
        egg.setDone(true); // ★変更 2件目を完了にします。
        todos.add(egg); // ★変更 2件目をリストに追加します。
        server.createContext("/", exchange -> { // 「/」へのアクセスが来たときの処理を書きます。
            String path = exchange.getRequestURI().getPath(); // アクセスされたパスを取り出します。
            String method = exchange.getRequestMethod(); // GETかPOSTかを取り出します。
            String message;
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8"); // 通常の応答はUTF-8の文字として返します。
            if (path.equals("/add") && method.equals("POST")) { // フォームから送られたTodoを受け取ります。
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8); // 送られた内容をUTF-8の文字として読みます。
                String value = body.substring(5); // 先頭の「todo=」を除きます。
                String title = URLDecoder.decode(value, StandardCharsets.UTF_8); // ★変更 日本語などの文字を元に戻してtitleに入れます。
                if (!title.isEmpty()) { // ★変更 空の入力は追加しません。
                    todos.add(new Todo(nextId, title)); // ★変更 入力されたTodoをリストに追加します。
                    nextId++; // ★変更 次に振る番号を進めます。
                } // 空の入力かどうかの確認を終えます。
                exchange.getResponseHeaders().set("Location", "/"); // 戻り先を「/」に指定します。
                exchange.sendResponseHeaders(303, -1); // ブラウザに戻り先へ移動するよう伝えます。
                exchange.close(); // この応答を閉じます。
                return; // ここで追加の処理を終えます。
            } else if (path.equals("/done") && method.equals("GET")) { // ★追加 完了のリンクを処理します。
                String query = exchange.getRequestURI().getQuery(); // ★追加 URLからidを受け取ります。
                if (query != null && query.startsWith("id=") && query.length() > 3) { // ★追加 idがあるか確認します。
                    try { // ★追加 数字への変換を試します。
                        int id = Integer.parseInt(query.substring(3)); // ★追加 idを数に変えます。
                        for (Todo todo : todos) { // ★追加 Todoを1件ずつ調べます。
                            if (todo.getId() == id) { // ★追加 番号が一致するか確認します。
                                todo.setDone(true); // ★追加 一致したTodoを完了にします。
                                break; // ★追加 見つかったので調べるのを終えます。
                            } // ★追加 番号の確認を終えます。
                        } // ★追加 Todoを調べ終えます。
                    } catch (NumberFormatException e) { // ★追加 数字でなければ何も変えません。
                    } // ★追加 数字への変換の処理を終えます。
                } // ★追加 idの確認を終えます。
                exchange.getResponseHeaders().set("Location", "/"); // ★追加 戻り先を指定します。
                exchange.sendResponseHeaders(303, -1); // ★追加 一覧へ戻します。
                exchange.close(); // ★追加 この応答を閉じます。
                return; // ★追加 完了の処理を終えます。
            } else if (path.equals("/delete") && method.equals("GET")) { // ★追加 削除のリンクを処理します。
                String query = exchange.getRequestURI().getQuery(); // ★追加 URLからidを受け取ります。
                if (query != null && query.startsWith("id=") && query.length() > 3) { // ★追加 idがあるか確認します。
                    try { // ★追加 数字への変換を試します。
                        int id = Integer.parseInt(query.substring(3)); // ★追加 idを数に変えます。
                        todos.removeIf(todo -> todo.getId() == id); // ★追加 一致したTodoを取り除きます。
                    } catch (NumberFormatException e) { // ★追加 数字でなければ何も変えません。
                    } // ★追加 数字への変換の処理を終えます。
                } // ★追加 idの確認を終えます。
                exchange.getResponseHeaders().set("Location", "/"); // ★追加 戻り先を指定します。
                exchange.sendResponseHeaders(303, -1); // ★追加 一覧へ戻します。
                exchange.close(); // ★追加 この応答を閉じます。
                return; // ★追加 削除の処理を終えます。
            } else if (path.equals("/")) { // 「/」で入力フォームとTodoの一覧を表示します。
                String html = "<form method='post' action='/add'><input name='todo'><button>追加</button></form><ul>";
                for (Todo todo : todos) { // ★変更 Todoを1件ずつ取り出します。
                    String mark = ""; // ★変更 未完了なら印を付けません。
                    if (todo.isDone()) { // ★変更 完了しているか確認します。
                        mark = " ✔"; // ★変更 完了の印を付けます。
                    } // ★変更 完了の確認を終えます。
                    html += "<li>" + todo.getTitle() + mark + " <a href='/done?id=" + todo.getId() + "'>完了</a> <a href='/delete?id=" + todo.getId() + "'>削除</a></li>"; // ★追加 タイトル、印、完了と削除のリンクを足します。
                } // すべてのTodoを足したので繰り返しを終えます。
                html += "</ul>"; // 箇条書きのHTMLを閉じます。
                message = html; // 作ったHTMLを応答に使います。
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8"); // 「/」はHTMLとして返します。
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

class Todo { // ★変更 Todoのデータを表すクラスです。
    private final int id; // ★変更 Todoの番号です。
    private final String title; // ★変更 やることです。
    private boolean done; // ★変更 終わったかどうかです。

    Todo(int id, String title) { // ★変更 番号とやることを受け取ります。
        this.id = id; // ★変更 番号を保存します。
        this.title = title; // ★変更 やることを保存します。
        this.done = false; // ★変更 最初は未完了にします。
    } // ★変更 コンストラクタを終えます。

    int getId() { // ★変更 番号を読み出します。
        return id; // ★変更 保存した番号を返します。
    } // ★変更 getIdを終えます。

    String getTitle() { // ★変更 やることを読み出します。
        return title; // ★変更 保存したやることを返します。
    } // ★変更 getTitleを終えます。

    boolean isDone() { // ★変更 終わったかどうかを読み出します。
        return done; // ★変更 保存した状態を返します。
    } // ★変更 isDoneを終えます。

    void setDone(boolean done) { // ★変更 終わったかどうかを書き換えます。
        this.done = done; // ★変更 新しい状態を保存します。
    } // ★変更 setDoneを終えます。
} // ★変更 Todoクラスを終えます。
