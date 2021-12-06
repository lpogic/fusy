# Interpreter Fusy dla JVM
Projekt kompilatora i środowiska uruchomieniowego dla aplikacji tworzonych w języku Fusy.

# Fusy
Eksperymentalny język programowania do szybkiego tworzenia niewielkich aplikacji.

# Próbka
(rozwiązanie zadania <a href="http://rosettacode.org/wiki/Dijkstra%27s_algorithm">http://rosettacode.org/wiki/Dijkstra%27s_algorithm</a>)
```
\\ 2x BACKSLASH = komentarz do końca linii
\\\
  3x BACKSLASH = komentarz zamykany kolejną sekwencją 3x BS
\\\
\\ Przypisz nowej zmiennej "startNode" wartość "a"(String)
#startNode = "a"
\\ Uniwersalna struktura danych "Subject" w postaci zbioru liter od "a" do "f"
#vertices = [ .a[] .b[] .c[] .d[] .e[] .f[] ]
#edges = []:merge[    \\ Wywołanie metody "merge" na pustym Subject
  .a [ .b [ 7 ] ]
  .a [ .c [ 9 ] ]
  .a [ .f [ 14 ] ]
  .b [ .c [ 10 ] ]
  .b [ .d [ 15 ] ]
  .c [ .d [ 11 ] ]
  .c [ .f [ 2 ] ]
  .d [ .e [ 6 ] ]
  .e [ .f [ 9 ] ]
]    \\ Utworzenie grafu przez podanie krawędzi


@record Link( Object from, Object to, Integer cost, Object lastNode) <    \\ Definicja rekordu "Link"

#input = []
#output = []
 \\ Iterowanie po całej kolekcji; Wywołanie metody "eachRaw" bez argumentów; z użyciem kropki i nawiasów
 for #v vertices.eachRaw()
  if !startNode.equals(v)
    \\ Przypisanie do "iclass" klasy Integer
    #iclass = @Integer
    input.set(new Link(startNode, v, edges.in(startNode).in(v).as(iclass, null), startNode))
  <    \\ Domknięcie if-a
<    \\ Domknięcie for-a

\\ Nagłówek funkcji "lowestCost", przyjmującej argument "input" typu Subject i zwracającej obiekt typu Link
@Link lowestCost(Subject input)
  \\ Funkcja lambda "@(l) return l.cost() != null <"
  #withCost = input.eachAs(@Link).select(@(l) return l.cost() != null <)
  \\ Wywołanie metody "first" bez argumentów; z użyciem dwukropka i z pominięciem nawiasów
  #lowest = withCost:first
  \\ for i if w formie jednoliniowej
  for #l withCost, if lowest:cost > l:cost, lowest = l
  return lowest
<    \\ Domknięcie funkcji "lowestCost"

\\ "@> lowestCost(input)" = Jednoliniowa funkcja lambda bez argumentów
for #lc pull(@> lowestCost(input))
  if lc == null, break
  input.unset(lc)
  output.put(lc:to, lc:lastNode)
  for #l edges.in(lc:to) 
    for #i input.eachAs(@Link) 
      #sumCost = l:in:asInt + lc:cost
      if i:to:equals(l:one) && (i:cost == null || i:cost > sumCost) 
        input.swap(i, new Link(i:from, i:to, sumCost, lc:to))
      <
    <
  <
<

@void printOutput(Subject output)
  #c = output:reverse:cascade
  for #o c 
    \\ Operator wyboru ( isTrue ? valueIfTrue !! valueIfFalse )
    out:print(c:firstFall ? "[ " !! "\n  ")
    \\ Formatowany String z użyciem #{ wyrażenie, które zostanie sprowadzone toString }
    out:print("#{ o:in:raw }-->#{ o:raw }")
  <
  out:println(" ]")
<

out:println("Output:")
printOutput(output)

@Subject shortestPath(Subject output, String from, String to)
  #path = [ to ]
  while !Objects.equals(path:last:raw, from), path.alter(output.in(path:last:raw))
  > path    \\ Krótka forma "return path"
<

@void printPath(Subject path)
  #c = path:reverse:cascade
  for #o c
    out:print(c:firstFall ? "[ " !! "-->")
    out:print(o:raw)
  <
  out:println(" ]")
<

out:println("\nShortest path from 'a' to 'e':")
printPath(shortestPath(output, "a", "e"))
out:println("\nShortest path from 'a' to 'f':")
printPath(shortestPath(output, "a", "f"))
```
wyjście:
```
Output:
[ d-->e
  c-->d
  c-->f
  a-->c
  a-->b ]

Shortest path from 'a' to 'e':
[ a-->c-->d-->e ]

Shortest path from 'a' to 'f':
[ a-->c-->f ]
```

Więcej przykładów na https://github.com/lpogic/fusy-rosetta-code
