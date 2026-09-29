// Itemsという名前のクラス（プログラムのまとまり）を作ります。
class Items { // この行でクラスの定義を始めます。
    // mainはプログラムを実行したとき最初に動く場所です。
    public static void main(String[] args) { // mainの処理を始めます。
        // todosという配列（複数の値を並べて入れる入れ物）を作ります。
        String[] todos = { "牛乳を買う", "", "パンを買う", "掃除をする" }; // 4件のTodoを入れます。
        // doneというboolean配列（trueかfalseを並べて入れる入れ物）を作ります。
        boolean[] done = { true, false, false, false }; // Todoごとの完了状態を入れます。
        // iを使うfor文（同じ処理を繰り返す書き方）で配列を順番に見ます。
        for (int i = 0; i < todos.length; i++) { // iが配列の件数より小さい間、繰り返します。
            // Todoが空文字列でない場合だけ、次の処理を行います。
            if (!todos[i].isEmpty()) { // 中身が空かどうかを調べます。
                // doneがtrueなら[済]を付け、falseなら何も付けません。
                String mark = done[i] ? "[済] " : ""; // 今のTodoに付ける印を決めます。
                // 今のTodoを<li>と</li>で囲んで、1行で表示します。
                System.out.println("<li>" + mark + todos[i] + "</li>"); // 印とTodoを出力します。
            } // 空でないTodoの出力を終えます。
        } // for文の繰り返しを終えます。
    } // mainの処理を終えます。
} // クラスの定義を終えます。
