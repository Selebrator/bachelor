The implementation accompanying my bachelors thesis on optimizations for the reachability problem of bounded Petri nets.

# Contents
 - Datastructures representing Petri nets using sparse vectors.
 - Datastructures representing Formulas of our reachability logic.
 - A breadth-first search algorithm using stubborn sets.
 - A breadth-first search algorithm using a symbolic encoding implemented with BDDs.
 - A depth-first search algorithm using a Bloom filter, a probabilistic datastructures.
 - Some structural reductions.
 - Parsers for many file types used in the [Petri net model checking contest](https://mcc.lip6.fr/)

Most algorithms are available with [APT](https://github.com/CvO-Theory/apt)'s and our own datastructures.

# Notable dependencies
 - [APT](https://github.com/CvO-Theory/apt) for its datastructures and parsers.
 - [Google Guava](https://github.com/google/guava) for utility functions and their Bloom filter implementation.
 - [javabdd](https://github.com/com-github-javabdd/com.github.javabdd) as the BDD library.
 - Java 16.

# How to build
Run
```sh
./gradlew jar
```
in a posix shell. This will build the program at `build/libs/bachelor-1.0-SNAPSHOT.jar`.

# How to use
Example usage:
Execute
```
java -jar bachelor-1.0-SNAPSHOT.jar --net "Sudoku-PT-BN04" --formulas "0,4" --solver "probabilistic" --timeout "20" --structural "10"
```
in a directory structure like this:
```
.
├── bachelor-1.0-SNAPSHOT.jar
└── Sudoku-PT-BN04
    ├── model.pnml
    └── ReachabilityCardinality.xml
```

All input files must be in the directory named as specified with the ``--net`` parameter.
The Petri net must be specified in [Place/Transition Net PNML format](https://www.pnml.org/) in the file named `model.pnml`.
The reachability formulas must be specified in the [MCC Property Language ReachabilityCardinality format](https://mcc.lip6.fr/2020/pdf/MCC2020-formula_manual.pdf).

The program accepts these parameters:
 - `--net` "Name of the net's directory"
 - `--formulas` "Comma separated list of formulas to test. Specified by occurrence index in the ReachabilityCardinality.xml file, starting from 0.
 - `--solver` "Name of the approach to use". Possible values are "base", "stubborn", "bdd", "probabilistic".
 - `--timeout` "Timeout per formula in seconds"
 - `--parallel`: Set this flag to enable parallel processing of formulas (not multithreading)
 - `--structural` "Timeout for structural reductions in seconds. Must be less then the overall timeout".

The `--parallel` and `--structural` options are incompatible.
`--solver "stubborn"` with `--parallel` is not possible.