/**
 * Converts an architecture name to a PascalCase task-name suffix.
 *
 * Examples:
 *  - `"x86-64"` → `"X8664"`
 *  - `"x86"`    → `"X86"`
 *  - `"arm64"`  → `"Arm64"`
 */
fun String.toTaskSuffix() = replace("-", "").replaceFirstChar { it.uppercase() }
