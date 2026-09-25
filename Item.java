// Itemという名前のクラス（プログラムのまとまり）を定義します。
public class Item {
    // mainメソッド（プログラムが最初に実行する場所）を定義します。
    public static void main(String[] args) {
        // titleというString型（文字列を入れる型）の変数を作ります。
        String title = "牛乳を買う";
        // +（文字列をつなぐ記号）で、1行分のHTML文字列を作ります。
        String html = "<li>" + title + "</li>";
        // 作ったHTML文字列をターミナル（文字を表示する画面）に出力します。
        System.out.println(html);
        boolean done = false;

        System.out.println(done);
        int count = 3;
        System.out.println("いま" + count + "件");

    }
}
