%% Author: jakob
%% Created: 24 nov 2009
%% Description: TODO: Add description to erlide_indent_tests
-module(erlide_indent_tests).

%%
%% Include files
%%

-include_lib("eunit/include/eunit.hrl").

%%
%% test Functions
%%

-define(Test_indent(SIndent, S),
        ?_assertEqual(SIndent,
                      erlide_indent:indent_lines(S, 0, length(S), 8, false, []))).

simple_function_test_() ->
    S = "a() ->\nb.\n",
    SIndent = "a() ->\n    b.\n",
    ?Test_indent(SIndent, S).

expressions_test_() ->
    S = "#r{a=a,\nb=b, [a,\nb],\n{a, b,\nc, fn(a, \nb)}},",
    SIndent = "#r{a=a,\n   b=b, [a,\n         b],\n   {a, b,\n    c, fn(a, \n          b)}},",
    ?Test_indent(SIndent, S).

try_catch_test_() ->
    S =
        ""++
            "cmd(Cmd, From, Args, Modules) ->\ntry\ncase get(logging) of\non ->\n"++
            "put(log, get(log)++[{Cmd, Args}]);\n_ ->\nok\nend,\n"++
            "case do_cmd(Cmd, Args, Modules) of\n{R, NewMods} ->\nreply(Cmd, From, R),\n"++
            "NewMods;\nNewMods ->\nreply(Cmd, From, ok),\nNewMods\nend\ncatch\n"++
            "exit:Error ->\nreply(Cmd, From, {exit, Error}),\nModules;\n"++
            "error:Error ->\nreply(Cmd, From, {error, Error}),\nModules\nend.",
    SIndent =
        ""++
            "cmd(Cmd, From, Args, Modules) ->\n"++
            "    try\n"++
            "        case get(logging) of\n"++
            "            on ->\n"++
            "                put(log, get(log)++[{Cmd, Args}]);\n"++
            "            _ ->\n"++
            "                ok\n"++
            "        end,\n"++
            "        case do_cmd(Cmd, Args, Modules) of\n"++
            "            {R, NewMods} ->\n"++
            "                reply(Cmd, From, R),\n"++
            "                NewMods;\n"++
            "            NewMods ->\n"++
            "                reply(Cmd, From, ok),\n"++
            "                NewMods\n"++
            "        end\n"++
            "    catch\n"++
            "        exit:Error ->\n"++
            "            reply(Cmd, From, {exit, Error}),\n"++
            "            Modules;\n"++
            "        error:Error ->\n"++
            "            reply(Cmd, From, {error, Error}),\n"++
            "            Modules\n"++
            "    end.",
    ?Test_indent(SIndent, S).

binary_1_test_() ->
    S =
        ""++
            "f() ->\n"++
            "<<1,\n"++
            "2>>.",
    SIndent =
        ""++
            "f() ->\n"++
            "    <<1,\n"++
            "      2>>.",
    ?Test_indent(SIndent, S).

%% http://www.assembla.com/spaces/erlide/tickets/595-indentation---doesn-t-handle-binaries-with-macros-or-expressions
binary_2_test_() ->
    S =
        ""++
            "g() ->\n"++
            "<<?M,\n"++
            "1>>.",
    SIndent =
        ""++
            "g() ->\n"++
            "    <<?M,\n"++
            "      1>>.",
    ?Test_indent(SIndent, S).

spec_test_() ->
    S = ""++
            "-spec start_link(config()) ->\n"++
            "{ok, pid()}.",
    SIndent = ""++
                  "-spec start_link(config()) ->\n"++
                  "          {ok, pid()}.",
    ?Test_indent(SIndent, S).

spec_2_test_() ->
    S = ""++
            "-spec start_link(config()) ->\n"++
            "{ok, pid()}.\n"++
            "f()->\n"++
            "ok.",
    SIndent = ""++
                  "-spec start_link(config()) ->\n"++
                  "          {ok, pid()}.\n"++
                  "f()->\n"++
                  "    ok.",
    ?Test_indent(SIndent, S).

spec_3_test_() ->
    S = ""++
            "-spec(start_link(config()) ->\n"++
            "{ok, pid()}).\n"++
            "f()->\n"++
            "ok.",
    SIndent = ""++
                  "-spec(start_link(config()) ->\n"++
                  "          {ok, pid()}).\n"++
                  "f()->\n"++
                  "    ok.",
    ?Test_indent(SIndent, S).

export_test_() ->
    S = ""++
            "-export([f/1,\n"++
            "f/2]).",
    SIndent = ""++
                  "-export([f/1,\n"++
                  "         f/2]).",
    ?Test_indent(SIndent, S).


%% binary comprehensions
%% http://www.assembla.com/spaces/erlide/tickets/729-indent--can-t-handle-binary-compehensions
binary_3_test_() ->
    S = ""++
            "foo(BS) ->\n"++
            "S = [A || <<A>> <= BS],\n"++
            "ok.",
    SIndent = ""++
                  "foo(BS) ->\n"++
                  "    S = [A || <<A>> <= BS],\n"++
                  "    ok.",
    ?Test_indent(SIndent, S).

%%
%% http://www.assembla.com/spaces/erlide/tickets/787-indent---confused-by-macros-in-case-clauses
macros_in_predicates_test_() ->
    S = ""++
            "foo() ->\n"++
            "case A of\n"++
            "?B when C == ?D ;\n"++
            "E == F ->",
    SIndent = ""++
                  "foo() ->\n"++
                  "    case A of\n"++
                  "        ?B when C == ?D ;\n"++
                  "                E == F ->",
    ?Test_indent(SIndent, S).

%%
type_test_() ->
    S = "" ++
            "-type mod_deps() :: dict().\n"++
            "a() ->\n"++
            "ok.\n",
    SIndent = "" ++
                  "-type mod_deps() :: dict().\n"++
                  "a() ->\n"++
                  "    ok.\n",
    ?Test_indent(SIndent, S).

%% http://www.assembla.com/spaces/erlide/tickets/936-indent--macros-in-list-comprehensions
macro_in_lc_test_() ->
    S = "" ++
            "b() ->\n"++
            "[?X(A) || X <-L],\n"++
            "a.\n",
    I = "" ++
            "b() ->\n"++
            "    [?X(A) || X <-L],\n"++
            "    a.\n",
    ?Test_indent(I, S).

%% http://www.assembla.com/spaces/erlide/tickets/776
%% indentation: receive..after is wrong
indent_after_test_() ->
    S = "" ++
            "a()->\n"++
            "receive\n"++
            "X ->\n"++
            "ok\n"++
            "after 500 ->\n"++
            "error\n"++
            "end.\n",
    I = "" ++
            "a()->\n"++
            "    receive\n"++
            "        X ->\n"
            "            ok\n"++
            "    after 500 ->\n"++
            "            error\n"++
            "    end.\n",
    ?Test_indent(I, S).

%% http://www.assembla.com/spaces/erlide/tickets/1083-indentation--bad-after--spec-with-when-clause
indent_spec_with_when_test_() ->
    S = "" ++
            "-spec a(T) -> ok when T::term().\n"++
            "a(apa) ->\n"++
            "ok.\n",
    I = "" ++
            "-spec a(T) -> ok when T::term().\n"++
            "a(apa) ->\n"++
            "    ok.\n",
    ?Test_indent(I, S).

%% http://assembla.com/spaces/erlide/tickets/1151-indent--fails-for-catch-with-guards
indent_catch_with_guards_test_() ->
    S = "" ++
            "f() ->\n"++
            "try\n"++
            "a\n"++
            "catch\n"++
            "A when is_tuple(A) ->\n"++
            "A\n"++
            "end.\n",
    I = "" ++
            "f() ->\n"++
            "    try\n"++
            "        a\n"++
            "    catch\n"++
            "        A when is_tuple(A) ->\n"++
            "            A\n"++
            "    end.\n",
    ?Test_indent(I, S).

indent_newline_char_test_() ->
    S = "" ++
            "a()->\n"++
            "foo(x, $\n, y),\n"++
            "boo(),\n" ++
            "ok.\n",
    I = "" ++
            "a()->\n"++
            "    foo(x, $\n, y),\n"++
            "    boo(),\n" ++
            "    ok.\n",
    ?Test_indent(I, S).

indent_maps_test_() ->
    S = "" ++
            "a()->\n"++
            "foo,\n"++
            "#[\n" ++
            "a=>b\n" ++
            "],\n" ++
            "ok.\n",
    I = "" ++
            "a()->\n"++
            "    foo,\n"++
            "    #[\n" ++
            "      a=>b\n" ++
            "     ],\n" ++
            "    ok.\n",
    ?Test_indent(I, S).

%%
%% Local Functions
%%

%% test_indent(SIndent, S) ->
%%     ?_assertEqual(SIndent, erlide_indent:indent_lines(S, 0, length(S), 8, false, [])).
