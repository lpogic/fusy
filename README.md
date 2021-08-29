# Fusy
Język programowania Fusy. Skryptowy, statycznie typowany, zbudowany na bazie Javy ( kompilowany do pliku .java ). Stworzony dla zabawy. Użycie na własne ryzyko :)
# Przykład
(rozwiązanie zadania <a href="http://rosettacode.org/wiki/Dijkstra%27s_algorithm">http://rosettacode.org/wiki/Dijkstra%27s_algorithm</a>)
```
#vertices = #["a", "b", "c", "d", "e", "f"]
#edges = #[
  #a [ #b [ 7 ] ]
  #a [ #c [ 9 ] ]
  #a [ #f [ 14 ] ]
  #b [ #c [ 10 ] ]
  #b [ #d [ 15 ] ]
  #c [ #d [ 11 ] ]
  #c [ #f [ 2 ] ]
  #d [ #e [ 6 ] ]
  #e [ #f [ 9 ] ]
]
#startNode = "a"

@record Link( Object from, Object to, Integer cost, Object lastNode) <

#input = #[]
#output = #[]
 for #v vertices.eachRaw() 
  if !startNode.equals(v) 
    input.set(new Link(startNode, v, edges.in(startNode).in(v).as(Integer@, null), startNode))
  <
<

@Link lowestCost(Subject input)
  #withCost = input.eachAs(Link@).select(#(l) l.cost() != null)
  #lowest = withCost.first()
  for #l withCost, if lowest.cost() > l.cost(), lowest = l <<
  return lowest
<

for #lc until(#() lowestCost(input), null)
  input.unset(lc)
  output.put(lc:to, lc:lastNode)
  for #l edges.in(lc:to) 
    for #i input.eachAs(Link@) 
      #sumCost = l:in:asInt + lc.cost()
      if i:to:equals(l:one) && (i.cost() == null || i.cost() > sumCost) 
        input.swap(i, new Link(i:from, i:to, sumCost, lc:to))
      <
    <
  <
<

@void printOutput(Subject output)
  #c = output:reverse:cascade()
  for #o c 
    print(c.firstFall() ? "[ " !! "\n  ")
    print("#[o:in:raw]-->#[o:raw]")
  <
  println(" ]")
<

println("Output:")
printOutput(output)

@Subject shortestPath(Subject output, String from, String to)
  #path = #[to]
  while !Objects.equals(path:last:raw, from)
    path.alter(output.in(path:last:raw))
  <
  return path
<

@void printPath(Subject path)
  #c = path:reverse:cascade()
  for #o : c
    print(c.firstFall() ? "[ " !! "-->")
    print(o:raw)
  <
  println(" ]")
<

println("\nShortest path from 'a' to 'e':")
printPath(shortestPath(output, "a", "e"))
println("\nShortest path from 'a' to 'f':")
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
