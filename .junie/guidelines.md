Project-specific guidelines:
- Run `gradle check` or `All tests` IDE configuration before and after making changes.

Process:
- Check if the feature already exists.
- Strictly follow test-driven development as if you meant it using the steps below:
  - Add a little test
  - Run all the tests and watch the new one fail
  - Make a little change
  - Run all the tests and watch the new one pass
  - Refactor to remove duplication
  - Repeat until done
- Do not write production code without a failing test
- Minimise the amount of code that doesn't compile
- Minimise the time when code doesn't compile or tests don't pass
- After finishing the changes when the tests are passing, check if there are any warnings in the IDE and fix them
- Prefer consistency to making something better only in one part of the code.

Code style:
- Declare public functions, classes first at the top of the file.
- Don't write comments unless the code is doing something unusual, and the comment explains why it was done this way.
- Remove unused variables, parameters, functions, classes
- Optimise imports
- Use full variable and parameter, e.g. for `Request` type use `request` instead of `req`
- Inline local variables with a single usage (if this doesn't make code worse in some other way)
- Assert on the whole object rather than each field individually (unless there is a reason to assert on each field)
- Make sure the style of the new code matches existing code.

Kotlin code style:
- prefer functional code style when working with collections 
  - use `forEach` instead of `for` loops
  - avoid using `continue` and `break`
- prefer read-only collections to mutable collections
- import enum values if possible
- use expression functions if possible
- use not-nullable types if possible
