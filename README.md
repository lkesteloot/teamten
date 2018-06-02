This is my main Java codebase. It contains some applications and some one-off projects.

* **chess**: A chess-playing program. See the driver in `scripts/chess` with
  test puzzles. It's pretty weak, but still beats me handily.

* **jawa**: A library for converting HTML (or any text) template into a Java
  class for static build analysis.

* **mario**: A reverse side-scroller. You click where you want the ball to go,
  and it figures out how to do it. Try it with `./gradlew mario`.

* **render**: A ray-tracer. This was mostly so I could experiment with ambient
  occlusion.

* **tictactoe**: This is an unfinished experiment to see if the computer can
  pick moves in Tic-Tac-Toe that are most likely to cause human error. Normally
  a Tic-Tac-Toe-playing program will see the entire tree, realize it's a tie
  for best play, and play anything. But surely some moves will have obvious
  replies and some non-obvious best replies, and this attempts to figure that
  out. Turns out that Tic-Tac-Toe is too simple a game for this. Chess is
  probably too complex to effectively evaluate.

The `scripts` directory is a set of Java programs that can be run using my
`java_launcher` script. This script compiles and caches the class file, making
it pretty easy to write short single-file Java programs without the overhead of
a build script.

