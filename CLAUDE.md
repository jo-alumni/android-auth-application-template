# rule

- `.claude/rules/` 配下に規約(ルール)ファイルを追加・編集する際は、必ず先頭にYAML frontmatterを
  つける。frontmatterには少なくとも以下のキーを含める。
  - `description`: そのルールが何を定めているかの一文サマリ
  - `globs`: そのルールが適用される対象ファイルのglobパターン(配列)
  - `alwaysApply`: 常に適用するかどうか(通常は `false`)
