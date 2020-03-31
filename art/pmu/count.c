#include<stdio.h>
int main(int argc, char *argv[]){

	long long int x = 0;
	unsigned long long z = 1ULL<<63;
	printf("%llu\n", z);
	z--;
	long long int max = z;
	printf("max = %lli\n", max);
	while(x < max){
		x++;
	}
	printf("%lli", x);
}
