package com.github.rohansuri.art;


// TODO: do a better job at refactoring iterator utilites?

class IteratorUtils {

    static <K, V> boolean shouldInvalidateNext(LastReturned<K, V> lastReturned, Path<K, V> path){
        // no next (lastReturned == root)
        if(lastReturned.pathIndex != -1 &&
                // is parent of lastReturned ancestor of next
                path.path.size() > lastReturned.pathIndex &&
                lastReturned.uplink.parent == path.path.get(lastReturned.pathIndex) &&
                // even if Node256 shrinks, it shrinks into Node48
                // and cursor position stays valid
                !(lastReturned.uplink.parent.node instanceof Node256) &&
                // node48 but would shrink
                (lastReturned.uplink.parent.node instanceof Node48 &&
                        lastReturned.uplink.parent.node.noOfChildren == Node16.NODE_SIZE+1)
        ){
            // next comes from same parent as current
            return true;
        }
        return false;
    }

    // TODO: very poor semantics, return better?
    // called when common ancestor of lastReturned and next
    // and we need to invalidate next
    static <K, V> Uplink<K, V> deleteEntryAndResetNext(AdaptiveRadixTree<K, V> m, LastReturned<K, V> lastReturned, Path<K, V> path, boolean forward) {
        m.keyRemoved();

        // parent surely exists
        InnerNode parent = lastReturned.uplink.parent.node;

        if (parent.shouldShrink()) {
            lastReturned.uplink.parent.remove(forward);
				/*
					Node48 to Node16:
						find partial key's new position in Node16 (see shrinkAndGetCursor implementation)

					Node16 to Node4:
						cursor position stays same

					we need to replace pathIndex with cursor over new parent we've got after shrinking.
				 */

            Cursor c = lastReturned.uplink.parent.shrink();
            // new parent, use uplink to update grand parent's downlink to this new parent
            m.grandParentToNewParent(lastReturned.uplink, c.node);
            path.path.set(lastReturned.pathIndex, c);
            return path.uplink();
        }

        lastReturned.uplink.parent.remove();
        if (parent.size() == 1 && !parent.hasLeaf()) {
				/*
					forward case:
						(lastReturned, next)
						lastReturned points to the removed child
						next is this last child

					back case:
						(next, lastReturned)
						lastReturned points to removed child
						next is this last child
				 */
            m.grandParentToOnlyChild(lastReturned.uplink, (Node4) parent);
				/*
					path cannot be empty since parent surely exists

					path currently looks like:
						(...., common GP, common Parent, next InnerNode ...)
						or
						(...., common GP, common Parent, next leafNode i.e. path.to ...)

					with path compression we removed common parent.
					we need to reflect this change in path as well.
					no cursor changes since InnerNode references haven't changed, neither have the position.
					GP still on the same position, just points to next directly.

					next would already be a leafnode or InnerNode.

					if next is leaf, the new uplink is (...common GGP, common GP, leaf)
					else (...common GGP, common GP, next InnerNode)
				 */
            path.path.remove(lastReturned.pathIndex);
            return path.uplink();
        }
        else if (parent.size() == 0) {
            assert parent.hasLeaf();
            // same reasoning as above
            m.grandParentToNewParent(lastReturned.uplink, parent.getLeaf());
            path.path.remove(lastReturned.pathIndex);
            return path.uplink();
        }
        /*
				parent can only be of Node4, Node16 type.
					if forward:
						move back next's cursor by one

					if back:
						no need to move, since remove only shifts array elements left for positions "after" current
        */
        return null;
    }
}
