How to differentiate between zero value of bytes (which is allowed?) vs no value present.
Classic trouble of wanting to signify absence of value as well as allowing zero value.

So suppose we got a stretch of 8 bits which are all zero in the given key.
So that'd mean looking for the 0th key.
For Node 4, 16 no problem, all depends upon if we have 0 stored in any of the array elements.
And we'd have stored 0 explicitly only if someone actually added 0 as the key.
For 256, we index directly into the 0th element. If it has a valid pointer, then yes you have a matching 0 key.
What about node 48? since it's a byte value which is by default 0, it's zero value will always be pointing
to index 0 of the child pointers?
oh bytes can have values -128 to +127.
And valid values are only 0 to 47.
So we can initialize them with -1?
So no need to worry about default values I guess then?