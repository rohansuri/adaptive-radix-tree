#include<stdio.h>
#include<stdlib.h>

int main(int argc, char* argv[]){

	long long int max = (1ULL<<63)-1;
	int *p;
	long long int i = 0;
	for(; i < max; i++){
		p =(int*) malloc(1*sizeof(int));
		(*p)++;
		free(p);
	}
	
}
