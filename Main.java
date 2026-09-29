import java.util.ArrayList; // ArrayList（リストを入れる箱）を使えるようにします。
import java.util.List; // List（複数のデータを順番に並べるリスト）を使えるようにします。
// Mainという名前の、プログラムを実行するクラス（まとまり）です。
public class Main { // publicは、どこからでも使えることを表します。
    // mainは、プログラムを実行したとき最初に動く場所です。
    public static void main(String[] args) { // mainの処理を始めます。
        // Todoを入れるList（順番に並べる入れ物）を作ります。
        List<Todo> todos = new ArrayList<>(); // Todoを入れる空のリストです。
        // 未完了の「牛乳を買う」をリストに追加します。
        todos.add(new Todo("牛乳を買う", false)); // falseは「まだ済んでいない」です。
        // 完了した「ゴミを出す」をリストに追加します。
        todos.add(new Todo("ゴミを出す", true)); // trueは「済んでいる」です。
        // for（繰り返し）でリストからTodoを1件ずつ取り出します。
        for (Todo todo : todos) { // todoにリスト内のTodoが順番に入ります。
            // toItem()の結果をターミナル（文字を表示する画面）に出します。
            System.out.println(todo.toItem()); // Todoを1行で表示します。
        } // forの繰り返しを終えます。
    } // mainの処理を終えます。
} // Mainクラスの定義を終えます。
// Todoクラス（Todoのタイトルと完了状態をまとめる設計図）です。
class Todo { // Todoクラスの定義を始めます。
    // titleはTodoのタイトル（文字列）です。
    private final String title; // finalは、作った後に値を変えないことを表します。
    // doneはTodoが済んだかどうかを表します。
    private final boolean done; // booleanはtrueかfalseを入れる型です。
    // タイトルと完了状態を受け取ってTodoを作ります。
    Todo(String title, boolean done) { // これをコンストラクター（作成時の処理）と呼びます。
        this.title = title; // 受け取ったタイトルをこのTodoに保存します。
        this.done = done; // 受け取った完了状態をこのTodoに保存します。
    } // Todoを作る処理を終えます。
    // Todoを指定されたHTML風の1行に変換します。
    String toItem() { // String（文字列）を返すメソッド（処理）です。
        // 済んでいるTodoには[済]を付けて返します。
        if (done) { // doneがtrueかどうかを確認します。
            return "<li>[済] " + title + "</li>"; // 完了済みの1行を返します。
        } // if（条件分け）の処理を終えます。
        // 済んでいないTodoは、印を付けずに返します。
        return "<li>" + title + "</li>"; // 未完了の1行を返します。
    } // toItem()の処理を終えます。
} // Todoクラスの定義を終えます。