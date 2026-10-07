import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.sql.Connection; // ★ SQLiteへの接続に使います。
import java.sql.DriverManager; // ★ JDBCでデータベースを開きます。
import java.sql.PreparedStatement; // ★ 値を安全にSQLへ渡します。
import java.sql.ResultSet; // ★ SELECTの結果を読みます。
import java.sql.SQLException; // ★ データベースのエラーを扱います。
import java.sql.Statement; // ★ テーブル作成に使います。
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class App {
    private static final String DB_URL = "jdbc:sqlite:todos.db"; // ★ 保存先をSQLiteにします。
    private static final String PAGE_TOP = "<!doctype html><html lang='ja'><head>"
            + "<meta charset='UTF-8'><meta name='viewport' content='width=device-width,initial-scale=1'>"
            + "<title>Task Quest</title><style>"
            + "*{box-sizing:border-box}body{margin:0;background:#f3f5f9;color:#273346;font-family:system-ui,-apple-system,'Segoe UI',sans-serif;line-height:1.55}"
            + "a{color:#245da8;text-decoration:none}a:hover{text-decoration:underline}"
            + ".app-shell{max-width:880px;margin:32px auto;padding:32px;background:#fff;border:1px solid #e2e7ef;border-radius:18px;box-shadow:0 12px 32px #23344a12}"
            + ".app-header{display:flex;justify-content:space-between;align-items:center;gap:20px;padding-bottom:22px;margin-bottom:26px;border-bottom:1px solid #e5e9f0}"
            + ".brand{margin:0;color:#204879;font-size:1.65rem;letter-spacing:.05em;font-weight:700}.subtitle{margin:2px 0 0;color:#6d7887;font-size:.9rem}"
            + ".mode-nav,.footer-nav{display:flex;flex-wrap:wrap;gap:8px}.mode-nav a,.mode-nav span,.footer-nav a{padding:9px 14px;border-radius:10px;font-weight:700}"
            + ".mode-nav a{background:#f1f4f8;color:#46566a}.mode-nav .active{background:#e7f0ff;color:#205aa4}.footer-nav a{background:#f1f4f8}"
            + ".mission-shell{background:#fffaf6;border-color:#efd7c5}.mission-shell .brand{color:#a84530}.mission-shell .mode-nav .active{background:#fbe2d7;color:#a53a27}"
            + "button,input,select{font:inherit}button,.action{min-height:40px;border:0;border-radius:9px;padding:9px 14px;font-weight:700;cursor:pointer}"
            + "button{background:#2469bd;color:#fff}button:hover{filter:brightness(.94)}input,select{min-width:0;width:100%;height:42px;padding:9px 11px;border:1px solid #cad3df;border-radius:9px;background:#fff;color:#273346}"
            + "input:focus-visible,select:focus-visible,button:focus-visible,a:focus-visible{outline:3px solid #83b8f3;outline-offset:2px}"
            + ".top-panels{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));align-items:start;gap:16px;margin-bottom:14px}.top-panels>.panel{min-width:0;padding:16px;border:1px solid #e3e8f0;border-radius:14px;background:#f7f9fc}"
            + ".panel-heading{grid-column:1/-1;margin:0 0 2px;font-size:1rem;color:#34465e}.add-form{display:grid;grid-template-columns:1fr 1fr;align-content:start;gap:9px}.add-form input[name=todo],.add-form button{grid-column:1/-1}"
            + ".control-forms{display:grid;gap:12px}.control-form{display:flex;gap:8px;min-width:0}.control-form input,.control-form select{flex:1}.control-form button{white-space:nowrap}.category-form{display:block}.category-form label{display:block;margin-bottom:5px;color:#5b6980;font-size:.85rem;font-weight:700}"
            + ".results-area{display:grid;grid-template-columns:minmax(0,1fr) auto;column-gap:12px;align-items:start}.toolbar{grid-column:1;grid-row:1;display:flex;flex-wrap:wrap;align-items:center;gap:8px 12px;min-height:36px}"
            + ".filter-links,.sort-links{display:flex;flex-wrap:wrap;align-items:center;gap:5px}"
            + ".filter-links a,.sort-links a{display:inline-block;padding:6px 11px;border-radius:8px;background:#e9eef5;color:#39546f;font-weight:650}.filter-links a.active,.sort-links a.active{background:#dceafe;color:#18559f}"
            + ".danger-bulk{margin:0}.danger-bulk button{background:#b94d4d;font-size:.82rem;min-height:32px;padding:5px 10px}"
            + ".todo-list,.mission-list{list-style:none;margin:0;padding:0;display:grid;gap:14px}.todo-list{grid-column:1/-1;grid-row:3;gap:9px}.todo-card,.mission-item{min-width:0;padding:18px;border:1px solid #dfe5ed;border-radius:14px;background:#fff;box-shadow:0 3px 12px #2733460a;overflow-wrap:anywhere}.todo-card{padding:12px 14px}"
            + ".todo-head{display:flex;align-items:center;flex-wrap:wrap;gap:10px}.todo-title,.mission-title{font-size:1.1rem;font-weight:750;color:#25354b}.todo-meta,.mission-meta{display:flex;flex-wrap:wrap;gap:8px;margin:10px 0;color:#627187;font-size:.88rem}.todo-meta{align-items:center;gap:5px 7px;margin:6px 0 9px;font-size:.82rem}"
            + ".todo-meta span,.mission-meta span{padding:3px 9px;background:#f1f4f8;border-radius:7px}.todo-meta span{padding:2px 7px}.state-badge{padding:3px 9px;border-radius:999px;background:#e8f3ed;color:#247448;font-size:.76rem;font-weight:700}.todo-meta .state-badge{border-radius:999px;background:#e8f3ed;color:#247448}.todo-meta .state-badge.done{background:#e9edf2;color:#586575}"
            + ".todo-card.is-done{background:#f8f9fb}.is-done .todo-title{text-decoration:line-through;color:#6a7685}.todo-card.due-today{border:2px solid #d95c5c;background:#fff0f0}"
            + ".todo-card.due-tomorrow{border:2px solid #d6b43c;background:#fffbea}"
            + ".todo-actions{display:flex;flex-wrap:wrap;gap:6px}.todo-actions .action,.todo-actions .deployed{min-height:30px;padding:4px 9px;font-size:.8rem}.action{display:inline-flex;align-items:center;justify-content:center;text-decoration:none;font-size:.85rem}.action:hover{text-decoration:none;filter:brightness(.94)}"
            + ".action.done{background:#e0f1e5;color:#1d7040}.action.edit{background:#e6edf6;color:#315678}.action.delete{background:#fde9e9;color:#a93838}.action.mission{background:#f4e8fa;color:#744293}"
            + ".deployed{display:inline-flex;align-items:center;min-height:40px;background:#c9463f;color:#fff;border-radius:9px;padding:9px 14px;font-size:.85rem;font-weight:700;cursor:default;user-select:none}"
            + ".section-heading{grid-column:1/-1;grid-row:2;display:flex;align-items:center;flex-wrap:wrap;gap:6px 16px;margin:12px 0 9px;font-size:1.1rem;color:#34465e}.section-heading h2{margin:0;font-size:inherit}.deadline-legend{display:inline-flex;flex-wrap:wrap;gap:4px 12px;color:#627187;font-size:.75rem;font-weight:500}.deadline-legend-item{display:inline-flex;align-items:center;gap:5px}.deadline-legend-item::before{content:'';width:10px;height:10px;border-radius:3px;flex:none}.deadline-legend-item.tomorrow::before{background:#fff0b6;border:1px solid #c9a735}.deadline-legend-item.today::before{background:#ffe1e1;border:1px solid #d26464}.count-status{grid-column:2;grid-row:1;justify-self:end;margin:0;padding:6px 10px;border:1px solid #dce6f3;border-radius:8px;background:#f2f7fd;color:#385b82;font-size:.82rem;font-weight:700;white-space:nowrap}"
            + ".mission-panel{padding:26px;border:1px solid #edcbb5;border-radius:16px;background:#fff;box-shadow:0 4px 18px #91482a12}mission-panel h1{margin:0;color:#aa4630;font-weight:inherit};mission-panel h1{margin:0;color:#aa4630;letter-spacing:.06em}.mission-panel h2{margin:8px 0 14px;font-size:1.1rem;color:#6a4b3d}"
            + ".mission-progress{margin:0 0 18px;color:#755f51;font-weight:700}.mission-item{border-color:#eddacb;background:#fffdfb}.mission-item .action{background:#e2efe5;color:#1c7040}.mission-meta{margin-bottom:12px}"
            + ".meter-row{margin:16px 0}.meter-label{margin:0 0 7px;font-weight:800;color:#674b3e}.gauge{width:100%;max-width:360px;height:16px;background:#e7e5e3;border-radius:999px;overflow:hidden}.gauge span{display:block;height:100%;background:#dc9444;border-radius:999px}"
            + ".level-up{margin:14px 0;padding:10px 14px;border-radius:12px;font-size:1.4rem;font-weight:800;text-align:center;"
            + "background:linear-gradient(90deg,#ff5f6d,#ffc371,#fff36b,#6ee7b7,#60a5fa,#a78bfa,#f472b6);"
            + "background-size:300% 300%;color:#fff;text-shadow:0 2px 6px #0004;box-shadow:0 0 18px #f59e0b55;"
            + "animation:levelUpRainbow 1.2s ease-in-out infinite alternate,levelUpPop .45s ease-out}"
            + "@keyframes levelUpRainbow{0%{background-position:0% 50%}100%{background-position:100% 50%}}"
            + "@keyframes levelUpPop{0%{transform:scale(.75);opacity:0}70%{transform:scale(1.08);opacity:1}100%{transform:scale(1)}}"
            + ".level-meter .gauge span{background:#c65a43}.complete-panel{text-align:center;padding:48px 24px;background:linear-gradient(150deg,#fff9ee,#fff)}.complete-panel h1{font-size:2rem}.complete-panel .gauge{margin:auto}"
            + ".empty-state{padding:28px;border:1px dashed #dfbfa8;border-radius:13px;background:#fff9f3;text-align:center;color:#725747}.empty-state p{margin:5px 0}.footer-nav{margin-top:24px}"
            + ".edit-form{display:grid;gap:12px;max-width:560px}.edit-form button{justify-self:start}.edit-card h1{margin-top:0}"
            + "@media(max-width:700px){.app-shell{margin:16px;padding:22px}.app-header{align-items:flex-start;flex-direction:column}.results-area{grid-template-columns:1fr}.toolbar{grid-column:1;grid-row:1}.count-status{grid-column:1;grid-row:2;justify-self:start;margin-top:8px}.section-heading{grid-row:3}.todo-list{grid-row:4}}"
            + "@media(max-width:600px){.top-panels{grid-template-columns:1fr}}"
            + "@media(max-width:520px){.app-shell{margin:0;min-height:100vh;border-radius:0;padding:18px}.add-form{grid-template-columns:1fr}.add-form button{width:100%}.control-form{flex-wrap:wrap}.control-form button{width:100%}.todo-card,.mission-item{padding:12px}.mission-panel{padding:18px}}"
            + "@media(max-width:600px){.normal-shell{padding:14px}.normal-shell .app-header{gap:8px;padding-bottom:10px;margin-bottom:12px}.normal-shell .brand{font-size:1.4rem}.normal-shell .mode-nav a,.normal-shell .mode-nav span{padding:6px 9px}.normal-shell .top-panels{gap:8px;margin-bottom:10px}.normal-shell .top-panels>.panel{padding:12px}.normal-shell .panel-heading{margin-bottom:0;font-size:.95rem}.normal-shell .add-form{grid-template-columns:1fr;gap:6px}.normal-shell .control-forms{gap:8px}.normal-shell .control-form{flex-wrap:nowrap;gap:6px}.normal-shell .control-form button{width:auto;flex:none}.normal-shell .category-form label{margin-bottom:3px}.normal-shell .top-panels input,.normal-shell .top-panels select{height:40px;padding:6px 9px}.normal-shell .top-panels button{min-height:40px;padding:6px 10px}.normal-shell .add-form button{width:auto;justify-self:start}.normal-shell .toolbar{gap:5px 8px;min-height:0}.normal-shell .filter-links a,.normal-shell .sort-links a{padding:4px 7px;font-size:.84rem}.normal-shell .section-heading{margin:8px 0 5px}.normal-shell .count-status{margin-top:5px;padding:4px 8px}.normal-shell .todo-list{gap:7px}.normal-shell .todo-card{padding:9px 10px}.normal-shell .todo-title{font-size:1rem}.normal-shell .todo-meta{gap:4px;margin:3px 0 6px;font-size:.76rem}.normal-shell .todo-meta span{padding:2px 5px}.normal-shell .todo-actions{gap:4px}.normal-shell .todo-actions .action,.normal-shell .todo-actions .deployed{min-height:30px;padding:4px 7px;font-size:.76rem}}"
            + "</style></head><body>";
    private static final String PAGE_END = "</main></body></html>";
    private static final Map<String, MissionSession> MISSION_SESSIONS = new ConcurrentHashMap<>();

    private static class MissionSession {
        final Set<Integer> selected = new HashSet<>();
        final Set<Integer> completedInRun = new HashSet<>();
        int earnedCount;
        int lastLevel = 1;
        boolean active;
    }

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
                    ResultSet results = statement.executeQuery("SELECT title, done, due_date FROM todos ORDER BY id")) { // 全Todoを読みます。
                StringBuilder json = new StringBuilder("["); // JSON配列を始めます。
                while (results.next()) { // Todoを1件ずつ処理します。
                    if (json.length() > 1)
                        json.append(','); // 2件目以降に区切りを入れます。
                    json.append("{\"title\":\"").append(jsonEscape(results.getString("title"))) // タイトルをJSONへ追加します。
                            .append("\",\"done\":").append(results.getInt("done") != 0) // 完了状態を真偽値で追加します。
                            .append(",\"due_date\":");
                    String dueDate = results.getString("due_date");
                    if (dueDate == null) {
                        json.append("null");
                    } else {
                        json.append('"').append(jsonEscape(dueDate)).append('"');
                    }
                    json.append('}'); // 1件分のJSONを閉じます。
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
        if (path.equals("/mission") && method.equals("GET")) {
            MissionSession session = missionSession(exchange);
            synchronized (session) {
                session.active = true;
            }
            redirect(exchange, "/mission/play");
            return;
        } else if (path.equals("/mission/reset") && method.equals("GET")) {
            MissionSession session = missionSession(exchange);
            synchronized (session) {
                session.completedInRun.clear();
                session.earnedCount = 0;
                session.active = true;
            }
            redirect(exchange, "/mission/play");
            return;

        } else if (path.equals("/mission/continue") && method.equals("GET")) {
            MissionSession session = missionSession(exchange);
            synchronized (session) {
                session.completedInRun.clear();
                session.active = true;
            }
            redirect(exchange, "/mission/play");
            return;
        } else if (path.equals("/mission/select") && method.equals("GET")) {
            Integer id = queryId(exchange);
            if (id != null) {
                try (Connection connection = DriverManager.getConnection(DB_URL);
                        PreparedStatement statement = connection.prepareStatement(
                                "SELECT done, due_date FROM todos WHERE id = ?")) {
                    statement.setInt(1, id);
                    try (ResultSet results = statement.executeQuery()) {
                        if (results.next() && results.getInt("done") == 0
                                && !isDueToday(results.getString("due_date"))) {
                            MissionSession session = missionSession(exchange);
                            synchronized (session) {
                                session.selected.add(id);
                            }
                        }
                    }
                }
            }
            redirect(exchange);
            return;
        } else if (path.equals("/mission/play") && method.equals("GET")) {
            MissionSession session = missionSession(exchange);
            String html;
            synchronized (session) {
                if (!session.active) {
                    redirect(exchange, "/mission");
                    return;
                }
                StringBuilder missions = new StringBuilder("<ul class='mission-list'>");
                int remaining = 0;
                try (Connection connection = DriverManager.getConnection(DB_URL);
                        Statement statement = connection.createStatement();
                        ResultSet results = statement.executeQuery(
                                "SELECT id, title, done, due_date, category FROM todos ORDER BY id")) {
                    while (results.next()) {
                        int id = results.getInt("id");
                        String dueDate = results.getString("due_date");
                        if (results.getInt("done") != 0
                                || (!session.selected.contains(id) && !isDueToday(dueDate))) {
                            continue;
                        }
                        remaining++;
                        String category = results.getString("category");
                        missions.append("<li class='mission-item'><span class='mission-title'>")
                                .append(escapeHtml(results.getString("title")))
                                .append("</span><div class='mission-meta'>")
                                .append(category == null || category.isEmpty() ? ""
                                        : "<span>カテゴリ: " + escapeHtml(category) + "</span>")
                                .append(dueDate == null || dueDate.isEmpty() ? ""
                                        : "<span>締切: " + escapeHtml(dueDate) + "</span>")
                                .append("</div><a class='action' href='/mission/done?id=").append(id)
                                .append("'>達成</a></li>");
                    }
                }
                missions.append("</ul>");
                int completed = session.completedInRun.size();
                int total = remaining + completed;
                int level = Math.min(5, 1 + session.earnedCount / 3);
                boolean levelUp = level > session.lastLevel;
                session.lastLevel = level;
                int expPercent = level == 5 ? 100 : (session.earnedCount % 3) * 100 / 3;
                String gauges = "<div class='meter-row exp-meter'><p class='meter-label'>EXP</p>"
                        + gauge(expPercent)
                        + "</div>"
                        + "<p class='meter-label'>LEVEL " + level + "</p>";

                String levelUpMessage = levelUp
                        ? "<div class='level-up'>LEVEL UP!</div>"
                        : "";
                if (total > 0 && remaining == 0) {
                    html = "<section class='mission-panel complete-panel'><h1>MISSION COMPLETE!</h1>"
                            + "<p>おめでとう！すべてのミッションを達成しました！</p>"
                            + "<p class='mission-progress'>" + total + "件中" + completed + "件達成</p>"
                            + gauges
                            + levelUpMessage
                            + "<p><strong>ミッションモードを継続しますか？</strong></p>"
                            + "<div class='mission-complete-actions'>"
                            + "<a class='action' href='/mission/continue'>継続する</a>"
                            + "<a class='action delete' href='/mission/reset'>最初から</a>"
                            + "</div>"
                            + "</section>";
                } else {
                    html = "<section class='mission-panel'><h1>MISSION MODE</h1><h2>今日のミッション</h2>"
                            + "<p class='mission-progress'>" + total + "件中" + completed + "件達成</p>"
                            + gauges
                            + levelUpMessage
                            + (total == 0
                                    ? "<div class='empty-state'><p>現在出撃中のミッションはありません</p>"
                                            + "<p>通常モードから『ミッションへ』を選択してください</p></div>"
                                    : missions.toString())
                            + "</section>";
                }
            }
            exchange.getResponseHeaders().set("Cache-Control", "no-store");
            send(exchange, 200, PAGE_TOP + "<main class='app-shell mission-shell'>"
                    + "<header class='app-header'><div><p class='brand'>TASK QUEST</p>"
                    + "<p class='subtitle'>今日のTodoをミッション形式で進め、EXPを獲得してレベルアップできます。</p></div>"
                    + "<nav class='mode-nav'><a href='/'>通常モード</a><span class='active'>ミッションモード</span></nav></header>"
                    + html + "<nav class='footer-nav'><a href='/mission'>ミッションを開き直す</a>"
                    + "<a href='/'>通常モードに戻る</a></nav>" + PAGE_END, "text/html");
            return;
        } else if (path.equals("/mission/done") && method.equals("GET")) {
            MissionSession session = missionSession(exchange);
            Integer id = queryId(exchange);
            synchronized (session) {
                if (session.active && id != null) {
                    try (Connection connection = DriverManager.getConnection(DB_URL);
                            PreparedStatement lookup = connection.prepareStatement(
                                    "SELECT done, due_date FROM todos WHERE id = ?")) {
                        lookup.setInt(1, id);
                        try (ResultSet results = lookup.executeQuery()) {
                            if (results.next() && results.getInt("done") == 0
                                    && (session.selected.contains(id) || isDueToday(results.getString("due_date")))) {
                                try (PreparedStatement update = connection.prepareStatement(
                                        "UPDATE todos SET done = 1 WHERE id = ? AND done = 0")) {
                                    update.setInt(1, id);
                                    if (update.executeUpdate() == 1) {
                                        if (session.completedInRun.add(id)) {
                                            session.earnedCount++;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            redirect(exchange, "/mission/play");
            return;
        } else if (path.equals("/add") && method.equals("POST")) {
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
                int deleted;
                try (Connection connection = DriverManager.getConnection(DB_URL);
                        PreparedStatement statement = connection.prepareStatement(
                                "DELETE FROM todos WHERE id = ?")) { // ★ DELETEを準備します。
                    statement.setInt(1, id); // ★ IDをパラメータとして渡します。
                    deleted = statement.executeUpdate(); // ★ 1件削除します。
                }
                if (deleted == 1) {
                    forgetTodo(id);
                }
            }
            redirect(exchange);
            return;
        } else if (path.equals("/delete-completed") && method.equals("POST")) {
            List<Integer> deletedIds = new ArrayList<>();
            try (Connection connection = DriverManager.getConnection(DB_URL);
                    Statement statement = connection.createStatement()) {
                try (ResultSet results = statement.executeQuery("SELECT id FROM todos WHERE done = 1")) {
                    while (results.next()) {
                        deletedIds.add(results.getInt("id"));
                    }
                }
                statement.executeUpdate("DELETE FROM todos WHERE done = 1");
            }
            for (int id : deletedIds) {
                forgetTodo(id);
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
                    String html = PAGE_TOP + "<main class='app-shell edit-card'>"
                            + "<header class='app-header'><div><p class='brand'>TASK QUEST</p>"
                            + "<p class='subtitle'>Todoを編集</p></div>"
                            + "<nav class='mode-nav'><a href='/'>通常モード</a><a href='/mission'>ミッションモード</a></nav></header>"
                            + "<h1>Todoを編集</h1><form class='edit-form' method='post' action='/update'>"
                            + "<input type='hidden' name='id' value='" + id + "'>"
                            + "<input name='title' aria-label='Todoタイトル' value='"
                            + escapeHtml(results.getString("title")) + "'>"
                            + "<input type='date' name='due_date' aria-label='締切日' value='"
                            + escapeHtml(dueDate == null ? "" : dueDate) + "'>"
                            + "<input name='category' aria-label='カテゴリ' placeholder='カテゴリ' value='"
                            + escapeHtml(category == null ? "" : category) + "'>"
                            + "<button>更新</button></form><nav class='footer-nav'><a href='/'>一覧に戻る</a></nav>"
                            + PAGE_END;
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
            String keywordQuery = keyword.isEmpty() ? ""
                    : "&amp;q=" + URLEncoder.encode(keyword, StandardCharsets.UTF_8);
            String categoryQuery = category.isEmpty() ? ""
                    : "&amp;category=" + URLEncoder.encode(category, StandardCharsets.UTF_8);
            List<String> categoryOptions = new ArrayList<>();
            try (Connection connection = DriverManager.getConnection(DB_URL);
                    Statement statement = connection.createStatement();
                    ResultSet categories = statement.executeQuery("SELECT DISTINCT category FROM todos "
                            + "WHERE category IS NOT NULL AND TRIM(category) <> '' ORDER BY category COLLATE NOCASE")) {
                while (categories.next()) {
                    categoryOptions.add(categories.getString("category"));
                }
            }
            StringBuilder html = new StringBuilder(
                    PAGE_TOP + "<main class='app-shell normal-shell'>"
                            + "<header class='app-header'><div><h1 class='brand'>TASK QUEST</h1>"
                            + "<p class='subtitle'>毎日のTodoを、見やすく整理</p>"
                            + "<p class='mission-guide'><strong>今日やるTodoに集中したいときは、ミッションモードへ！</strong><br>"
                            + "※「ミッションへ」ボタンで、挑戦したいTodoを事前にストックできます。</p></div>"
                            + "<nav class='mode-nav'><span class='active'>通常モード</span>"
                            + "<a href='/mission'>ミッションモード</a></nav></header>"
                            + "<div class='top-panels'><section class='panel add-panel'>"
                            + "<form class='add-form' method='post' action='/add'>"
                            + "<h2 class='panel-heading'>Todo追加</h2>"
                            + "<input name='todo' aria-label='Todoタイトル' placeholder='Todoタイトル'>"
                            + "<input type='date' name='due_date' aria-label='締切日'>"
                            + "<input name='category' aria-label='カテゴリ' placeholder='カテゴリ'>"
                            + "<button>追加</button></form></section><section class='panel search-panel controls'>"
                            + "<h2 class='panel-heading'>検索・絞り込み</h2><div class='control-forms'>");
            html.append(
                    "<form class='control-form' method='get' action='/'><input name='q' aria-label='検索' placeholder='Todoを検索' value='")
                    .append(escapeHtml(keyword)).append("'><button>検索</button>")
                    .append("<input type='hidden' name='filter' value='").append(filter).append("'>")
                    .append("<input type='hidden' name='category' value='").append(escapeHtml(category)).append("'>");
            if (!sort.isEmpty()) {
                html.append("<input type='hidden' name='sort' value='").append(sort).append("'>");
            }
            html.append("</form><form class='control-form category-form' method='get' action='/'>"
                    + "<label for='category-filter'>カテゴリ</label>"
                    + "<select id='category-filter' name='category' onchange='this.form.submit()'>"
                    + "<option value=''");
            if (category.isEmpty()) {
                html.append(" selected");
            }
            html.append(">すべて</option>");
            for (String option : categoryOptions) {
                html.append("<option value='").append(escapeHtml(option)).append("'");
                if (option.equals(category)) {
                    html.append(" selected");
                }
                html.append(">").append(escapeHtml(option)).append("</option>");
            }
            html.append("</select><noscript><button>絞り込み</button></noscript>")
                    .append("<input type='hidden' name='filter' value='").append(filter).append("'>")
                    .append("<input type='hidden' name='q' value='").append(escapeHtml(keyword)).append("'>");
            if (!sort.isEmpty()) {
                html.append("<input type='hidden' name='sort' value='").append(sort).append("'>");
            }
            html.append("</form></div></section></div><section class='results-area'><div class='toolbar'>"
                    + "<div class='filter-links'>")
                    .append("<a class='").append(filter.equals("all") ? "active" : "").append("' href='/?filter=all")
                    .append(sortQuery).append(keywordQuery).append(categoryQuery).append("'>全部</a>")
                    .append("<a class='").append(filter.equals("todo") ? "active" : "").append("' href='/?filter=todo")
                    .append(sortQuery).append(keywordQuery).append(categoryQuery).append("'>未完了</a>")
                    .append("<a class='").append(filter.equals("done") ? "active" : "").append("' href='/?filter=done")
                    .append(sortQuery).append(keywordQuery).append(categoryQuery).append("'>完了</a></div>")
                    .append("<div class='sort-links'>")
                    .append("<a class='").append(sort.equals("new") ? "active" : "").append("' href='/?filter=")
                    .append(filter).append("&amp;sort=new").append(keywordQuery)
                    .append(categoryQuery)
                    .append("'>新しい順</a>")
                    .append("<a class='").append(sort.equals("name") ? "active" : "").append("' href='/?filter=")
                    .append(filter).append("&amp;sort=name").append(keywordQuery)
                    .append(categoryQuery)
                    .append("'>名前順</a></div>")
                    .append("<form class='danger-bulk' method='post' action='/delete-completed' ")
                    .append("onsubmit=\"return confirm('完了済みのTodoをすべて削除しますか？');\">")
                    .append("<button>完了済みを一括削除</button></form></div>")
                    .append("<div class='section-heading'><h2>Todo一覧</h2><span class='deadline-legend'><span class='deadline-legend-item tomorrow'>明日締切</span><span class='deadline-legend-item today'>今日締切</span></span></div><ul class='todo-list'>"); // ★
                                                                                                                                                                                                                                                         // 一覧を組み立てます。
            LocalDate today = LocalDate.now();
            MissionSession session = missionSession(exchange);
            Set<Integer> selected;
            synchronized (session) {
                selected = new HashSet<>(session.selected);
            }
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
                        boolean isDone = results.getInt("done") != 0;
                        String mark = isDone ? " ✔" : ""; // ★ DBの完了状態を使います。
                        String dueClass = "";
                        if (dueDate != null && !dueDate.isEmpty()) {
                            try {
                                LocalDate deadline = LocalDate.parse(dueDate);
                                if (deadline.equals(today)) {
                                    dueClass = "due-today";
                                } else if (deadline.equals(today.plusDays(1))) {
                                    dueClass = "due-tomorrow";
                                }
                            } catch (DateTimeParseException e) {
                                // 日付として読めない既存データは通常表示にします。
                            }
                        }
                        html.append("<li class='todo-card");
                        if (!dueClass.isEmpty()) {
                            html.append(' ').append(dueClass);
                        }
                        if (isDone) {
                            html.append(" is-done");
                        }
                        html.append("'><div class='todo-head'><span class='todo-title'>").append(title)
                                .append("</span></div><div class='todo-meta'><span class='state-badge")
                                .append(isDone ? " done" : "")
                                .append("'>").append(isDone ? "完了済み" + mark : "未完了")
                                .append("</span>")
                                .append(todoCategory == null || todoCategory.isEmpty()
                                        ? "<span>カテゴリなし</span>"
                                        : "<span>カテゴリ: " + escapeHtml(todoCategory) + "</span>")
                                .append(dueDate == null || dueDate.isEmpty()
                                        ? "<span>締切なし</span>"
                                        : "<span>締切: " + escapeHtml(dueDate) + "</span>")
                                .append("</div><div class='todo-actions'>")
                                .append("<a class='action edit' href='/edit?id=").append(id).append("'>編集</a>")
                                .append("<a class='action done' href='/done?id=").append(id)
                                .append("'>完了</a><a class='action delete' href='/delete?id=").append(id)
                                .append("'>削除</a>");
                        if (!isDone) {
                            if (selected.contains(id) || isDueToday(dueDate)) {
                                html.append("<span class='deployed'>出撃中</span>");
                            } else {
                                html.append("<a class='action mission' href='/mission/select?id=").append(id)
                                        .append("'>ミッションへ</a>");
                            }
                        }
                        html.append("</div></li>"); // ★ SELECTしたTodoを表示します。
                    }
                    html.append("</ul><p class='count-status'>" + totalCount + "件中" + doneCount + "件 完了</p></section>");
                }
            }
            html.append(PAGE_END);
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

    private static MissionSession missionSession(HttpExchange exchange) {
        String cookies = exchange.getRequestHeaders().getFirst("Cookie");
        if (cookies != null) {
            for (String cookie : cookies.split(";")) {
                String[] parts = cookie.trim().split("=", 2);
                if (parts.length == 2 && parts[0].equals("mission_session")) {
                    MissionSession session = MISSION_SESSIONS.get(parts[1]);
                    if (session != null) {
                        return session;
                    }
                }
            }
        }
        String id = UUID.randomUUID().toString();
        MissionSession session = new MissionSession();
        MISSION_SESSIONS.put(id, session);
        exchange.getResponseHeaders().add("Set-Cookie", "mission_session=" + id + "; Path=/; HttpOnly; SameSite=Lax");
        return session;
    }

    private static void forgetTodo(int id) {
        for (MissionSession session : MISSION_SESSIONS.values()) {
            synchronized (session) {
                session.selected.remove(id);
                session.completedInRun.remove(id);
            }
        }
    }

    private static boolean isDueToday(String dueDate) {
        if (dueDate == null || dueDate.isEmpty()) {
            return false;
        }
        try {
            return LocalDate.parse(dueDate).equals(LocalDate.now());
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    private static String gauge(int percent) {
        return "<div class='gauge'><span style='width:" + percent + "%'></span></div>";
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
        redirect(exchange, "/");
    }

    private static void redirect(HttpExchange exchange, String path) throws IOException {
        exchange.getResponseHeaders().set("Location", path);
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
