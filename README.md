This is my main Java library. It contains some utility classes,
some applications, and some one-off projects.

* **chess**: A chess-playing program. See the driver in `scripts/chess` with
  test puzzles. It's pretty weak, but still beats me handily.

* **hyphen**: A library for hyphenating words. Uses the TeX hyphenation file
  format.

* **image**: A library for image manipulation. This is quite extensive and of
  high quality.

* **jawa**: A library for converting HTML (or any text) template into a Java
  class for static build analysis.

* **mario**: A reverse side-scroller. You click where you want the ball to go,
  and it figures out how to do it. Try it with `./gradlew mario`.

* **markdown**: Simple Markdown parser for book-length material.

* **math**: Math classes for complex numbers, vectors, and matrices.

* **render**: A ray-tracer. This was mostly so I could experiment with ambient
  occlusion.

* **tictactoe**: This is an unfinished experiment to see if the computer can
  pick moves in Tic-Tac-Toe that are most likely to cause human error. Normally
  a Tic-Tac-Toe-playing program will see the entire tree, realize it's a tie
  for best play, and play anything. But surely some moves will have obvious
  replies and some non-obvious best replies, and this attempts to figure that
  out. Turns out that Tic-Tac-Toe is too simple a game for this. Chess is
  probably too complex to effectively evaluate.

* **typeset**: A typesetting library for converting the output of the Markdown
  parser to a PDF. Uses a TeX-like typesetting algorithm.

* **util**: A set of utility classes.

The `scripts` directory is a set of Java programs that can be run using my
`java_launcher` script. This script compiles and caches the class file, making
it pretty easy to write short single-file Java programs without the overhead of
a build script.

