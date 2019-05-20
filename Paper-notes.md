### Abstract

* ART is aimed at being useful as an index for in memory databases.
* Aim is to stay as performant as hash tables wrt to search time, but also support range queries, prefix lookups, min, max, top-K and be space efficient.
* Falls under the third class of search data structures (key based comparison search trees, hash tables, digital search trees)
* The three classes:  
    * O(log n)  
    * O(1) amortized  
    * O(k)

### Problems with B+ tree:
* Assume memory access time is uniform irrespective of memory location to access. But this is no longer true with growing cache sizes.
* They are key comparison based and hence lead to failed branch predications and hence CPU stalls.  
(Do a short experiment to prove the above two points?)

### Why hash tables are not used for database indexes?

* Scatter keys randomly, hence only support point queries.
* Expensive reorganization time (recopying, etc) when reaching current capacity limits.

### Why new data structures still creep up?
* Simply to keep up with the design/behaviour of modern hardware architectures.
* In an era of extremely large data sets, when `n` is growing faster than `k`, having a time complexity independent of `n` is very attractive.

### Ideas used in ART
* Path compression
* Lazy expansion
* For each local inner node, their sizes are dynamically chosen depending on the number of child nodes. Hence we optimize global space utilization.

### Related work
* k-ary trees reduce search time in search trees from log N base 2 to log N base k, where k is number of keys that fit into one SIMD vector.

### Why index structures for GPUs are not yet popular?
* They have expensive communication costs with main memory.
* Memory capacities of GPUs are limited (their cache sizes?)
* The high throughput they give is only when you issue hundreds of parallel queries.
(sounds like it is beneficial only when there's enough usage. Not optimal for light usag)

Section III and IV are most important to understand the implementation.

### Interesting properties of radix trees in comparison to search trees
* Height depends on length of keys.
* No rebalancing operations.
* All insertion orders lead to same tree.
* Root to leaf path represents the key of that key, hence keys are stored implicitly.

### Span of radix trees
* Span is the number of bits that form a single chunk in doing the comparison with a given key. Denoted by `s`.
* For `s` bit chunk, a node size of 2^s pointer to children is the best representation, since one can index into the pointer array and requires no comparisons.
* Choosing `s` determines the tree height.  
  For given keys of size k and span s, we'd have k/s levels.
* Higher the span, lesser the tree height and hence faster the search (why? isn't search time always O(k)?), but more the space consumption.

### Comparing height of radix trees with search trees
* If comparing only the height, radix tree's height is `k/s` while search trees usually have `log n base 2`.
* Therefore you use radix trees when k/s < log n base 2 i.e. when `2^(k/s) < n`.
* For the same given span `s`, radix trees and ART would have the same height, but the size of the nodes on every level would be different.
* Being adaptive, ART would consume a lot less space, which is why in turn it lets us choose a higher `s` to get smaller tree height.

### The main idea
* Use adaptive node sizes as and when needed.
* Use node types of 4, 16, 48, 256 and regrow when number of non-null children crosses node size and then becomes smaller when node is underfull.

### Inner nodes
* Span of 8 bits (1 byte) is chosen because 1 byte is directly addressable and hence doesn't require bitmasking/shifting.
* Each of the inner nodes allow efficient find, search, delete of the 1 byte partial key.
* To store the partial key and the value (child pointer), we use separate arrays. (See Node 48's structure to understand why separate)
* All inner nodes contain 16byte size header metadata to store node type, number of child pointers, compressed path.

#### Node 4
* Store 4 keys in sorted order.

#### Node 16
* Store 5-16 keys in sorted order.
* To search the partial 1 byte key, you could use binary search or SIMD instructions.

#### Node 48
* Have 256 element key index array but only 48 element child pointer array.
* This allows O(1) key search, which gives the value array index into which corresponding value lies.
* We save on memory by having space for only 48 pointers.
* Since key, value require different array sizes, we use separate arrays for all node size structures.

#### Node 256
* 256 element array upon whose indexing gives the child pointer directly without any indirection.

### Leaf nodes
* For keys, values of varying length, we store the value in a separate value leaf node.
* If keys are all the same length then the last inner node would instead be a value node of the required adaptive size. These are called multi-value leaves.
* If value size is less than 8 bytes then we store them in the inner node's child pointer field directly, hence saving an indirection. This is common for for database indexes, when they store the tuple identifier (usually an integer not more than 8 bytes). Hence we need to keep some metadata in the inner node to understand if it stores a child pointer or a value.

### Collapsing inner nodes
#### Lazy expansion
* Inner nodes are only created if they are required to distinguish at least two leaf nodes.
* Due to lazy expansion, path to leaf no longer represents the key and rather needs to be kept explicitly in the leaf node itself.

#### Path compression
* Remove all inner nodes which have only one child, i.e. out of all node pointer array indexes, only one is non-nil, so we compress the path.
* The compressed path can be stored in two ways:
    * Pessimistic: Store the preceding one-way nodes (partial key) in the final non compressed child node and do the comparison when searching.
    * Optimistic: Only keep a count of such a compressed partial key and skip over those many keys and finally compare the entire key in the leaf node.
* A hybrid approach is used and partial keys upto 8 bytes are stored in a pessimistic way and later the lookup switches to optimistic compressed path.

### Experiments done
* Random keys (dense and sparse)
* Zipf skewed keys
* Cache size impact: For 16M dense keys, when  increasing cache size, no of lookups per second increase.

### On the way questions
* Why do we need to store number of child pointers in inner node's header? And what is compressed path?

### Questions
* Why do database indexes require range queries?
* How is ART bounding space consumption per key to 52 bytes, even for arbitarily long keys?  
  Does it mean with it's path compression, lazy expansion, there can never be a dataset, that when inserted takes the tree height to the 53rd level?
* What is the limit of SIMD vector on my machine?
* What is memory bandwidth for CPUs, GPUs? Why is it more for GPUs?
* Why does radix tree's height matter? No matter what span you choose, it only affects the size right? Not the time. The time is fixed at O(k). Then what are we saying about radix vs search trees when we're comparing their heights?
* Why store node type and number of child pointers?
* How do radix trees store prefix keys? Foot and Football?
  Pointer from t would have a value node as well as a pointer to a node which has it's bth index pointing to next child having it's ath index pointing to...so on.

  We need to have one extra space in child pointer array to contain such null terminated keys (something [plar](https://github.com/plar/go-adaptive-radix-tree/blob/master/node.go#L56) is doing)  
  OR  
  Enforce users to mark their keys with terminating characters or something (as done by [armon](https://github.com/armon/libart/issues/12)

* Isn't lazy expansion just a special form of path compression?

### Implementation ideas
* Should we provide a flag to decide whether or not to shrink nodes when they are underfull?  
  No, ideally such a decision should be internal, solved by experiments.