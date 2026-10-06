import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        List<Todo> todos = new ArrayList<>();

        todos.add(new Todo("牛乳を買う", false));
        todos.add(new Todo("ゴミを出す", true));

        int doneCount = 0;

        for (Todo todo : todos) {
            if (todo.isDone()) {
                doneCount++;
            }
        }

        System.out.println(todos.size() + "件中" + doneCount + "件 完了");
    }
}

class Todo {
    private final String title;
    private final boolean done;

    Todo(String title, boolean done) {
        this.title = title;
        this.done = done;
    }

    boolean isDone() {
        return done;
    }

    String toItem() {
        if (done) {
            return "<li>[済] " + title + "</li>";
        }

        return "<li>" + title + "</li>";
    }
}