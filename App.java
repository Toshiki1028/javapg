import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection; // ★ SQLiteへの接続に使います。
import java.sql.DriverManager; // ★ JDBCでデータベースを開きます。
import java.sql.PreparedStatement; // ★ 値を安全にSQLへ渡します。
import java.sql.ResultSet; // ★ SELECTの結果を読みます。
import java.sql.SQLException; // ★ データベースのエラーを扱います。
import java.sql.Statement; // ★ テーブル作成に使います。

public class App {
    private static final String DB_URL = "jdbc:sqlite:todos.db"; // ★ 保存先をSQLiteにします。

    public static void main(String[] args) throws Exception {
        createTable(); // ★ 起動時にtodos表を用意します。
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/api/todos", exchange -> { // JSON一覧の入口を追加します。
            if (!exchange.getRequestURI().getPath().equals("/api/todos")) { // 対象のパスを確認します。
                send(exchange, 404, "ページが見つかりません", "text/plain"); // 別のパスは404にします。
                return; // 処理を終えます。
            } // パスの確認を終えます。
            if (!exchange.getRequestMethod().equals("GET")) { // GETだけを受け付けます。
                send(exchange, 405, "GETのみ利用できます", "text/plain"); // 別のメソッドは405にします。
                return; // 処理を終えます。
            } // メソッドの確認を終えます。
            try (Connection connection = DriverManager.getConnection(DB_URL); // SQLiteへ接続します。
                    Statement statement = connection.createStatement(); // SELECTを実行する準備をします。
                    ResultSet results = statement.executeQuery("SELECT title, done FROM todos ORDER BY id")) { // 全Todoを読みます。
                StringBuilder json = new StringBuilder("["); // JSON配列を始めます。
                while (results.next()) { // Todoを1件ずつ処理します。
                    if (json.length() > 1)
                        json.append(','); // 2件目以降に区切りを入れます。
                    json.append("{\"title\":\"").append(jsonEscape(results.getString("title"))) // タイトルをJSONへ追加します。
                            .append("\",\"done\":").append(results.getInt("done") != 0) // 完了状態を真偽値で追加します。
                            .append('}'); // 1件分のJSONを閉じます。
                } // 全Todoの処理を終えます。
                json.append(']'); // JSON配列を閉じます。
                byte[] body = json.toString().getBytes(StandardCharsets.UTF_8); // UTF-8の応答データにします。
                exchange.getResponseHeaders().set("Content-Type", "application/json"); // charsetを付けずにJSON型を指定します。
                exchange.sendResponseHeaders(200, body.length); // 応答の状態と長さを送ります。
                try (var output = exchange.getResponseBody()) { // 応答ストリームを確実に閉じます。
                    output.write(body); // JSONを送ります。
                } // 応答ストリームを閉じます。
            } catch (SQLException e) { // データベースエラーを処理します。
                e.printStackTrace(); // 詳細をサーバー側に記録します。
                send(exchange, 500, "データベースエラー", "text/plain"); // エラーを返します。
            } // JSON一覧の処理を終えます。
        }); // APIの入口の定義を終えます。
        server.createContext("/", exchange -> {
            try { // ★ SQLの失敗をHTTP 500として返します。
                handle(exchange); // ★ 各リクエストでデータベースを操作します。
            } catch (SQLException e) { // ★ データベースのエラーを処理します。
                e.printStackTrace(); // ★ 詳細はサーバー側に表示します。
                send(exchange, 500, "データベースエラー", "text/plain"); // ★ 利用者にエラーを知らせます。
            }
        });
        server.start();
        System.out.println("サーバー起動: http://localhost:8080 （止めるときは Ctrl+C）");
    }

    private static void createTable() throws SQLException { // ★ 表がなければ作成します。
        try (Connection connection = DriverManager.getConnection(DB_URL);
                Statement statement = connection.createStatement()) { // ★ 接続とStatementを閉じます。
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS todos "
                    + "(id INTEGER PRIMARY KEY, title TEXT, done INTEGER, due_date TEXT, category TEXT)");
            boolean hasDueDate = false;
            boolean hasCategory = false;
            try (ResultSet columns = statement.executeQuery("PRAGMA table_info(todos)")) {
                while (columns.next()) {
                    if (columns.getString("name").equals("due_date")) {
                        hasDueDate = true;
                    } else if (columns.getString("name").equals("category")) {
                        hasCategory = true;
                    }
                }
            }
            if (!hasDueDate) {
                statement.executeUpdate("ALTER TABLE todos ADD COLUMN due_date TEXT");
            }
            if (!hasCategory) {
                statement.executeUpdate("ALTER TABLE todos ADD COLUMN category TEXT");
            }
        }
    }

    private static void handle(HttpExchange exchange) throws IOException, SQLException { // ★ SQL例外も扱います。
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();
        if (path.equals("/add") && method.equals("POST")) {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            String title = "";
            String dueDate = "";
            String category = "";
            for (String parameter : body.split("&")) {
                try {
                    if (parameter.startsWith("todo=")) {
                        title = URLDecoder.decode(parameter.substring(5), StandardCharsets.UTF_8)
                                .replace('\r', ' ').replace('\n', ' ');
                    } else if (parameter.startsWith("due_date=")) {
                        dueDate = URLDecoder.decode(parameter.substring(9), StandardCharsets.UTF_8);
                    } else if (parameter.startsWith("category=")) {
                        category = URLDecoder.decode(parameter.substring(9), StandardCharsets.UTF_8)
                                .replace('\r', ' ').replace('\n', ' ');
                    }
                } catch (IllegalArgumentException e) {
                    // 不正なフォーム値は空として扱います。
                }
            }
            if (!title.isEmpty()) {
                try (Connection connection = DriverManager.getConnection(DB_URL);
                        PreparedStatement statement = connection.prepareStatement(
                                "INSERT INTO todos (title, done, due_date, category) VALUES (?, 0, ?, ?)")) {
                    statement.setString(1, title); // ★ 入力値をパラメータとして渡します。
                    statement.setString(2, dueDate);
                    statement.setString(3, category);
                    statement.executeUpdate(); // ★ 1件追加します。
                }
            }
            redirect(exchange);
            return;
        } else if (path.equals("/done") && method.equals("GET")) {
            Integer id = queryId(exchange); // ★ 更新対象のIDを読みます。
            if (id != null) {
                try (Connection connection = DriverManager.getConnection(DB_URL);
                        PreparedStatement statement = connection.prepareStatement(
                                "UPDATE todos SET done = 1 WHERE id = ?")) { // ★ UPDATEを準備します。
                    statement.setInt(1, id); // ★ IDをパラメータとして渡します。
                    statement.executeUpdate(); // ★ 完了状態を更新します。
                }
            }
            redirect(exchange);
            return;
        } else if (path.equals("/delete") && method.equals("GET")) {
            Integer id = queryId(exchange); // ★ 削除対象のIDを読みます。
            if (id != null) {
                try (Connection connection = DriverManager.getConnection(DB_URL);
                        PreparedStatement statement = connection.prepareStatement(
                                "DELETE FROM todos WHERE id = ?")) { // ★ DELETEを準備します。
                    statement.setInt(1, id); // ★ IDをパラメータとして渡します。
                    statement.executeUpdate(); // ★ 1件削除します。
                }
            }
            redirect(exchange);
            return;
        } else if (path.equals("/delete-completed") && method.equals("POST")) {
            try (Connection connection = DriverManager.getConnection(DB_URL);
                    Statement statement = connection.createStatement()) {
                statement.executeUpdate("DELETE FROM todos WHERE done = 1");
            }
            redirect(exchange);
            return;
        } else if (path.equals("/edit") && method.equals("GET")) {
            Integer id = queryId(exchange);
            if (id == null) {
                send(exchange, 404, "編集するTodoが見つかりません", "text/plain");
                return;
            }
            try (Connection connection = DriverManager.getConnection(DB_URL);
                    PreparedStatement statement = connection.prepareStatement(
                            "SELECT title, due_date, category FROM todos WHERE id = ?")) {
                statement.setInt(1, id);
                try (ResultSet results = statement.executeQuery()) {
                    if (!results.next()) {
                        send(exchange, 404, "編集するTodoが見つかりません", "text/plain");
                        return;
                    }
                    String dueDate = results.getString("due_date");
                    String category = results.getString("category");
                    String html = "<form method='post' action='/update'>"
                            + "<input type='hidden' name='id' value='" + id + "'>"
                            + "<input name='title' value='" + escapeHtml(results.getString("title")) + "'>"
                            + "<input type='date' name='due_date' value='"
                            + escapeHtml(dueDate == null ? "" : dueDate) + "'>"
                            + "<input name='category' placeholder='カテゴリ' value='"
                            + escapeHtml(category == null ? "" : category) + "'>"
                            + "<button>更新</button></form><a href='/'>一覧に戻る</a>";
                    send(exchange, 200, html, "text/html");
                }
            }
            return;
        } else if (path.equals("/update") && method.equals("POST")) {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            Integer id = null;
            String title = "";
            String dueDate = "";
            String category = "";
            for (String parameter : body.split("&")) {
                if (parameter.startsWith("id=")) {
                    try {
                        id = Integer.parseInt(parameter.substring(3));
                    } catch (NumberFormatException e) {
                        id = null;
                    }
                } else if (parameter.startsWith("title=")) {
                    try {
                        title = URLDecoder.decode(parameter.substring(6), StandardCharsets.UTF_8)
                                .replace('\r', ' ').replace('\n', ' ');
                    } catch (IllegalArgumentException e) {
                        title = "";
                    }
                } else if (parameter.startsWith("due_date=")) {
                    try {
                        dueDate = URLDecoder.decode(parameter.substring(9), StandardCharsets.UTF_8);
                    } catch (IllegalArgumentException e) {
                        dueDate = "";
                    }
                } else if (parameter.startsWith("category=")) {
                    try {
                        category = URLDecoder.decode(parameter.substring(9), StandardCharsets.UTF_8)
                                .replace('\r', ' ').replace('\n', ' ');
                    } catch (IllegalArgumentException e) {
                        category = "";
                    }
                }
            }
            if (id != null && !title.isEmpty()) {
                try (Connection connection = DriverManager.getConnection(DB_URL);
                        PreparedStatement statement = connection.prepareStatement(
                                "UPDATE todos SET title = ?, due_date = ?, category = ? WHERE id = ?")) {
                    statement.setString(1, title);
                    statement.setString(2, dueDate);
                    statement.setString(3, category);
                    statement.setInt(4, id);
                    statement.executeUpdate();
                }
            }
            redirect(exchange);
            return;
        } else if (path.equals("/") && method.equals("GET")) {
            String query = exchange.getRequestURI().getRawQuery();
            String filter = "all";
            String sort = "";
            String keyword = "";
            String category = "";
            if (query != null) {
                for (String parameter : query.split("&")) {
                    if (parameter.equals("filter=todo")) {
                        filter = "todo";
                    } else if (parameter.equals("filter=done")) {
                        filter = "done";
                    } else if (parameter.equals("filter=all")) {
                        filter = "all";
                    } else if (parameter.equals("sort=new")) {
                        sort = "new";
                    } else if (parameter.equals("sort=name")) {
                        sort = "name";
                    } else if (parameter.startsWith("q=")) {
                        try {
                            keyword = URLDecoder.decode(parameter.substring(2), StandardCharsets.UTF_8);
                        } catch (IllegalArgumentException e) {
                            keyword = "";
                        }
                    } else if (parameter.startsWith("category=")) {
                        try {
                            category = URLDecoder.decode(parameter.substring(9), StandardCharsets.UTF_8);
                        } catch (IllegalArgumentException e) {
                            category = "";
                        }
                    }
                }
            }
            String sql = "SELECT id, title, done, due_date, category FROM todos";
            if (filter.equals("todo")) {
                sql += " WHERE done = 0";
            } else if (filter.equals("done")) {
                sql += " WHERE done != 0";
            }
            if (!keyword.isEmpty()) {
                sql += filter.equals("all") ? " WHERE" : " AND";
                sql += " title LIKE ? ESCAPE '!'";
            }
            if (!category.isEmpty()) {
                sql += filter.equals("all") && keyword.isEmpty() ? " WHERE" : " AND";
                sql += " category = ?";
            }
            if (sort.equals("new")) {
                sql += " ORDER BY id DESC";
            } else if (sort.equals("name")) {
                sql += " ORDER BY title ASC";
            } else {
                sql += " ORDER BY id";
            }
            String sortQuery = sort.isEmpty() ? "" : "&amp;sort=" + sort;
            String keywordQuery = keyword.isEmpty() ? "" : "&amp;q=" + URLEncoder.encode(keyword, StandardCharsets.UTF_8);
            String categoryQuery = category.isEmpty() ? "" : "&amp;category=" + URLEncoder.encode(category, StandardCharsets.UTF_8);
            StringBuilder html = new StringBuilder(
                    "<form method='post' action='/add'><input name='todo'>"
                            + "<input type='date' name='due_date'><input name='category' placeholder='カテゴリ'>"
                            + "<button>追加</button></form>");
            html.append("<form method='post' action='/delete-completed' ")
                    .append("onsubmit=\"return confirm('完了済みのTodoをすべて削除しますか？');\">")
                    .append("<button>完了済みを一括削除</button></form>");
            html.append("<form method='get' action='/'><input name='q' value='")
                    .append(escapeHtml(keyword)).append("'><button>検索</button>")
                    .append("<input type='hidden' name='filter' value='").append(filter).append("'>")
                    .append("<input type='hidden' name='category' value='").append(escapeHtml(category)).append("'>");
            if (!sort.isEmpty()) {
                html.append("<input type='hidden' name='sort' value='").append(sort).append("'>");
            }
            html.append("</form><form method='get' action='/'><input name='category' placeholder='カテゴリで絞り込み' value='")
                    .append(escapeHtml(category)).append("'><button>カテゴリで絞り込み</button>")
                    .append("<input type='hidden' name='filter' value='").append(filter).append("'>")
                    .append("<input type='hidden' name='q' value='").append(escapeHtml(keyword)).append("'>");
            if (!sort.isEmpty()) {
                html.append("<input type='hidden' name='sort' value='").append(sort).append("'>");
            }
            html.append("</form>")
                    .append("<a href='/?filter=all").append(sortQuery).append(keywordQuery).append(categoryQuery)
                    .append("'>全部</a> | ")
                    .append("<a href='/?filter=todo").append(sortQuery).append(keywordQuery).append(categoryQuery)
                    .append("'>未完了</a> | ")
                    .append("<a href='/?filter=done").append(sortQuery).append(keywordQuery).append(categoryQuery)
                    .append("'>完了</a><br>")
                    .append("<a href='/?filter=").append(filter).append("&amp;sort=new").append(keywordQuery)
                    .append(categoryQuery)
                    .append("'>新しい順</a> | ")
                    .append("<a href='/?filter=").append(filter).append("&amp;sort=name").append(keywordQuery)
                    .append(categoryQuery)
                    .append("'>名前順</a><ul>"); // ★ 一覧を組み立てます。
            try (Connection connection = DriverManager.getConnection(DB_URL);
                    PreparedStatement statement = connection.prepareStatement(sql)) {
                int parameterIndex = 1;
                if (!keyword.isEmpty()) {
                    String escapedKeyword = keyword.replace("!", "!!").replace("%", "!%").replace("_", "!_");
                    statement.setString(parameterIndex++, "%" + escapedKeyword + "%");
                }
                if (!category.isEmpty()) {
                    statement.setString(parameterIndex, category);
                }
                try (ResultSet results = statement.executeQuery()) { // ★ 条件に応じたSELECTで一覧を取得します。
                    int totalCount = 0;
                    int doneCount = 0;
                    while (results.next()) { // ★ 取得した行を表示します。
                        totalCount++;

                        if (results.getInt("done") != 0) {
                            doneCount++;
                        }
                        int id = results.getInt("id"); // ★ DBのIDを使います。
                        String title = escapeHtml(results.getString("title")); // ★ タイトルをHTML用に変換します。
                        String dueDate = results.getString("due_date");
                        String todoCategory = results.getString("category");
                        String mark = results.getInt("done") != 0 ? " ✔" : ""; // ★ DBの完了状態を使います。
                        html.append("<li>").append(title).append(mark)
                                .append(todoCategory == null || todoCategory.isEmpty()
                                        ? " カテゴリなし" : " カテゴリ: " + escapeHtml(todoCategory))
                                .append(dueDate == null || dueDate.isEmpty()
                                        ? " 締切なし" : " 締切: " + escapeHtml(dueDate))
                                .append(" <a href='/edit?id=").append(id).append("'>編集</a>")
                                .append(" <a href='/done?id=").append(id)
                                .append("'>完了</a> <a href='/delete?id=").append(id)
                                .append("'>削除</a></li>"); // ★ SELECTしたTodoを表示します。
                    }
                    html.append("<p>" + totalCount + "件中" + doneCount + "件 完了</p>");
                }
            }
            html.append("</ul>");
            send(exchange, 200, html.toString(), "text/html"); // ★ DBから作った一覧を返します。
            return;
        }
        send(exchange, 404, "ページが見つかりません", "text/plain"); // ★ 未知のURLを処理します。
    }

    private static Integer queryId(HttpExchange exchange) { // ★ URLのIDを検証します。
        String query = exchange.getRequestURI().getQuery();
        if (query == null || !query.startsWith("id=")) {
            return null;
        }
        try {
            return Integer.parseInt(query.substring(3));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String escapeHtml(String value) { // ★ 保存したタイトルを安全に表示します。
        return value.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }

    private static String jsonEscape(String value) { // JSON文字列の中身をエスケープします。
        StringBuilder escaped = new StringBuilder(); // 変換後の文字をためます。
        for (int i = 0; i < value.length(); i++) { // 文字を順に調べます。
            char c = value.charAt(i); // 現在の文字を取り出します。
            switch (c) { // 特別な文字を判定します。
                case '"':
                    escaped.append("\\\"");
                    break; // 引用符をエスケープします。
                case '\\':
                    escaped.append("\\\\");
                    break; // バックスラッシュをエスケープします。
                case '\b':
                    escaped.append("\\b");
                    break; // バックスペースをエスケープします。
                case '\f':
                    escaped.append("\\f");
                    break; // 改ページをエスケープします。
                case '\n':
                    escaped.append("\\n");
                    break; // 改行をエスケープします。
                case '\r':
                    escaped.append("\\r");
                    break; // 復帰をエスケープします。
                case '\t':
                    escaped.append("\\t");
                    break; // タブをエスケープします。
                default: // それ以外の文字を処理します。
                    if (c < 0x20) { // 残りの制御文字を判定します。
                        escaped.append("\\u00").append(Character.forDigit(c >>> 4, 16)) // 上位の桁を書きます。
                                .append(Character.forDigit(c & 0x0f, 16)); // 下位の桁を書きます。
                    } else { // 通常の文字を処理します。
                        escaped.append(c); // そのまま追加します。
                    } // 制御文字の処理を終えます。
            } // 文字種の判定を終えます。
        } // 全文字の処理を終えます。
        return escaped.toString(); // エスケープ済み文字列を返します。
    } // 補助メソッドを終えます。

    private static void redirect(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Location", "/");
        exchange.sendResponseHeaders(303, -1);
        exchange.close();
    }

    private static void send(HttpExchange exchange, int status, String message, String type) throws IOException {
        byte[] body = message.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", type + "; charset=UTF-8");
        exchange.sendResponseHeaders(status, body.length);
        try (var output = exchange.getResponseBody()) {
            output.write(body);
        }
    }
}
