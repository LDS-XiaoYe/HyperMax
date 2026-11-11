## 重要规则

1. 写文档，README 不允许使用“不是，而是”句式，也不得在不恰当的地方或过多使用 “·”和破折号。
2. 代码不能写死偏移，要使用动态解析/动态寻址，要使用运行时特征识别，而不是符号表解析。
3. adb 安装时必须使用 --no-incremental 非增量安装。
4. 回答用户时使用简体中文、
5. 修改时做最小更改，不要过度设计

## 风格指南

- 尽可能避免使用 try / catch
- 除非可组合或可重用，否则请将所有内容放在一个函数中。
- Test actual implementation, do not duplicate logic into tests
- Avoid mocks as much as possible, you shouldn't be using globalThis.* at all unless it's the only option.
- Before adding complexity for a speculative or vanishingly unlikely race or security edge case, explain the concrete failure mode, likelihood, and complexity cost to the user and get their buy-in. Do not silently expand scope for theoretical robustness.
- Do not extract single-use helpers preemptively. Inline the logic at the call site unless the helper is reused, hides a genuinely complex boundary, or has a clear independent name that improves the caller.
- Validate unknown values once at the boundary that owns them. Pass typed values inward instead of repeating typeof value === "object" and property-existence checks. Do not defensively revalidate values already guaranteed by a schema, constructor, or internal type.
- Tests cannot run from repo root (guard: do-not-run-tests-from-root); run from package directories such as packages/core.