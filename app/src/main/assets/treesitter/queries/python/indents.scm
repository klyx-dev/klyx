; Blocks, expressions, collections, and statements that increase indentation level
[
  (import_from_statement)
  (generator_expression)
  (list_comprehension)
  (set_comprehension)
  (dictionary_comprehension)
  (tuple_pattern)
  (list_pattern)
  (binary_operator)
  (lambda)
  (concatenated_string)
  (list)
  (dictionary)
  (set)
  (parenthesized_expression)
  (for_statement)
  (if_statement)
  (while_statement)
  (try_statement)
  (function_definition)
  (class_definition)
  (with_statement)
  (match_statement)
  (case_clause)
  (argument_list)
  (parameters)
  (tuple)
  ] @indent.begin

; Dedicated structural markers that handle block shifts and alignment matching
[
  ")"
  "]"
  "}"
  (elif_clause)
  (else_clause)
  (except_clause)
  (finally_clause)
  ] @indent.branch

; Control flow breaks that trigger structural back-stepping
[
  (break_statement)
  (continue_statement)
  ] @indent.dedent

  ; Explicit inner target boundaries for ending expression folds cleanly
  (generator_expression ")" @indent.end)
  (list_comprehension "]" @indent.end)
  (set_comprehension "}" @indent.end)
  (dictionary_comprehension "}" @indent.end)
  (tuple_pattern ")" @indent.end)
  (list_pattern "]" @indent.end)

  ; Maintain auto handling across raw string arrays
  (string) @indent.auto