%%% Input Data %%%

int: r; % Number of rounds
int: objStep1; % Number of active Sboxes
int: KEY_BITS; % Number of bits in the key (in {128, 192, 256}
int: KC=KEY_BITS div 32; % Number of columns per round of key schedule (in {4, 6, 8})
  
%%% Variables %%%
array[0..r-1, 0..3, 0..3] of var 0..1:  DX; % State after ARK
array[0..r-1, 0..3, 0..3] of var 0..1:  DSX;% State after SB
array[0..r-1, 0..3, 0..3] of var 0..1:  DY; % State after SR
array[0..r-2, 0..3, 0..3] of var 0..1:  DZ; % State after MC
array[0..r-1, 0..3, 0..4] of var 0..1:  DK; % Key. The additional column (at index 4) corresponds to the column after SB, called delta SK in the paper 
array[0..r-1,0..3,0..4,0..r-1,0..4] of var 0..1: DIFF_K; % DIFF variables for the key
array[0..r-2,0..3,0..3,0..r-2,0..3] of var 0..1: DIFF_Y; % DIFF variables for the state before MC
array[0..r-2,0..3,0..3,0..r-2,0..3] of var 0..1: DIFF_Z; % DIFF variables for the state after MC
array[0..r-1,0..3] of var 0..4: colY; % colSRX[r][j] = The sum for i in 0..3 of DY[r][i][j]
array[0..r-1,0..4] of var 0..4: colK; %   colK[r][j] = The sum for i in 0..3 of DK[r][i][j]
%%% Constraints %%%

%%% Number of active Sboxes
constraint sum(i in 0..r-1, k in 0..3)(colY[i,k]) + sum(i in 0..r-1 where isSBRound(i))(colK[i,4])=objStep1; 

%%% Sums for the columns
constraint forall (i in 0..r-1, k in 0..4) (
           if k<4 then 
                    colY[i,k]=sum(j in 0..3)(DY[i,j,k])       
           else true endif /\
                    colK[i,k]=sum(j in 0..3)(DK[i,j,k]) 
);

%%% SubBytes in the key
constraint forall (i in 0..r-1, j in 0..3)( 
  if  isSBRound(i) then % There are Sboxes at round i, and the bytes that pass through these Sboxes are at column getNbCol(i)
    if KC<8 \/ i mod 2==1 then DK[i,j,4] = DK[i,(j+1) mod 4,getNbCol(i)]
    else DK[i,j,4] = DK[i,j,getNbCol(i)]  % Special case for AES 256, no rotword for the even rounds
    endif
  else % No Sbox at round i => DK[i,j,4] is not used and is set to 0
    DK[i,j,4]=0
  endif 
); 

%%% Add round key
constraint forall(i in 1..r-1,j in 0..3, k in 0..3) (
       XOR(DZ[i-1,j,k], DK[i,j,k], DX[i,j,k])    
);

%%% Mixcolumns
constraint forall(i in 0..r-2, k in 0..3) (
      colY[i,k]+sum(j in 0..3)(DZ[i,j,k]) in {0,5,6,7,8} 
);

%%% SubBytes in the text
constraint forall(i in 0..r-1,j in 0..3, k in 0..3) (
       DSX[i,j,k]=DX[i,j,k]
);        

%%% Shiftrows
constraint forall(i in 0..r-1,j in 0..3, k in 0..3) (
       DY[i,j,k]=DSX[i,j,(j+k) mod 4]
);        

%%% XOR constraint
predicate XOR(var 0..1: a,var 0..1: b,var 0..1: c) =
      a+b+c!=1 
;

% XOR of 3 key bytes
predicate XOR3 (array [1..3] of int: A, array [1..3] of int: B, array [1..3] of int: C) =
    if A[3]==5 \/ B[3]==5 \/ C[3] == 5 then DK[A[1]-1,A[2]-1, A[3]-1]+ DK[B[1]-1,B[2]-1, B[3]-1] + DK[C[1]-1,C[2]-1, C[3]-1] !=1 else true endif /\
    if A[3]<5 /\ B[3] < 5 then DIFF_K[A[1]-1, A[2]-1, A[3]-1, B[1]-1, B[3]-1] = DK[C[1]-1,C[2]-1, C[3]-1] else true endif /\ 
    if A[3]<5 /\ C[3] < 5 then DIFF_K[A[1]-1, A[2]-1, A[3]-1, C[1]-1, C[3]-1] = DK[B[1]-1,B[2]-1, B[3]-1] else true endif /\  
    if B[3]<5 /\ C[3] < 5 then DIFF_K[B[1]-1, B[2]-1, B[3]-1, C[1]-1, C[3]-1] = DK[A[1]-1,A[2]-1, A[3]-1] else true endif 
;

% XOR of 4 key bytes
predicate XOR4 (array [1..3] of int: A, array [1..3] of int: B, array [1..3] of int: C, array [1..3] of int: D) =
    if A[3]<5 /\ B[3] < 5 /\ C[3]< 5 /\ D[3] < 5 then 
     DIFF_K[A[1]-1, A[2]-1, A[3]-1, B[1]-1, B[3]-1] = DIFF_K[C[1]-1, C[2]-1, C[3]-1, D[1]-1, D[3]-1]  /\
     DIFF_K[A[1]-1, A[2]-1, A[3]-1, C[1]-1, C[3]-1] = DIFF_K[B[1]-1, B[2]-1, B[3]-1, D[1]-1, D[3]-1]  /\
     DIFF_K[A[1]-1, A[2]-1, A[3]-1, D[1]-1, D[3]-1] = DIFF_K[B[1]-1, B[2]-1, B[3]-1, C[1]-1, C[3]-1] 
    else 
    DK[A[1]-1,A[2]-1, A[3]-1]+ DK[B[1]-1,B[2]-1, B[3]-1] + DK[C[1]-1,C[2]-1, C[3]-1] + DK[D[1]-1,D[2]-1, D[3]-1] !=1  
    endif 
;

predicate DIFF1(int: i1, int: j, int: k1, int: i2, int: k2, int: i3, int: k3) = (
     DIFF_K[i1,j,k1,i3,k3] + DIFF_K[i1,j,k1,i2,k2] + DIFF_K[i2,j,k2,i3,k3] != 1 %transitivity
);

%%% Diff relations
%%%%% State
constraint forall(i1 in 1..r-1, j in 0..3, k1 in 0..3,i2 in 1..r-1, k2 in 0..3)( 
      DY[i1-1,j,k1]+DY[i2-1,j,k2]+ DIFF_Y[i1-1,j,k1,i2-1,k2] !=1 
  /\  DZ[i1-1,j,k1]+DZ[i2-1,j,k2]+ DIFF_Z[i1-1,j,k1,i2-1,k2] !=1
  /\   DIFF_K[i1,j,k1,i2,k2] + DIFF_Z[i1-1,j,k1,i2-1,k2] + DX[i1,j,k1] + DX[i2,j,k2] !=1 
   /\ (DIFF_Z[i1-1,j,k1,i2-1,k2]==DIFF_Z[i2-1,j,k2,i1-1,k1])   % Symmetry
   /\ (DIFF_Y[i1-1,j,k1,i2-1,k2]==DIFF_Y[i2-1,j,k2,i1-1,k1])   % Symmetry
   /\    forall (i3 in 1..r-1, k3 in 0..3 where i3>i2 \/ (i3==i2 /\ k3>k2)) ( 
           DIFF_Z[i1-1,j,k1,i3-1,k3] + DIFF_Z[i1-1,j,k1,i2-1,k2] + DIFF_Z[i2-1,j,k2,i3-1,k3] != 1 /\  %Transitivity
           DIFF_Y[i1-1,j,k1,i3-1,k3] + DIFF_Y[i1-1,j,k1,i2-1,k2] + DIFF_Y[i2-1,j,k2,i3-1,k3] != 1     %Transitivity
        )
);

%%%%% Key
constraint forall(i1 in 0..r-1, j in 0..3, k1 in 0..4,i2 in 0..r-1, k2 in 0..4 where  (isSBOutput(i1, k1) /\ isSBOutput(i2,k2) /\ (i1>i2 \/ (i1==i2 /\ k1>k2))))(
     (DIFF_K[i1,j,k1,i2,k2]==DIFF_K[i2,j,k2,i1,k1]) % Symmetry
     /\  (DIFF_K[i1,j,k1,i2,k2] + DK[i1,j,k1] + DK[i2,j,k2] != 1) % DIFF_K[A,B] => a!=b
);

%%%%%%%%%% Mixcolumns constraint
constraint forall(i1 in 1..r-1, j in 0..3, k1 in 0..3,i2 in 1..r-1, k2 in 0..3 where i2>i1 \/ (i2==i1 /\ k2>k1)) ( 
    sum(j in 0..3)(DIFF_Y[i1-1,j,k1, i2-1,k2]+DIFF_Z[i1-1,j,k1, i2-1,k2]) in       {0,5,6,7,8}
);


%%% Functions that define KeySchedule operations, depending on the size KEY_BITS of the key %%%
test isSB(int: i, int: k) = % True if the kth column of the key at round i goes through a SBox 
      (KEY_BITS==128 /\ k==3) %AES 128
  \/  (KEY_BITS==192 /\ (i*4+k) mod KC == KC-1) % AES 192
  \/  (KEY_BITS==256 /\ k==3 /\ i>0) %AES 256
;
test isXORedWithSB(int: i, int: k) = % True if the kth column of the key at round i goes through a SBox 
      (KEY_BITS==128 /\ k==0) %AES 128
  \/  (KEY_BITS==192 /\ (i*4+k) mod KC == 0) % AES 192
  \/  (KEY_BITS==256 /\ k==0 /\ i>1) %AES 256
;
test isInitialKey(int:i, int:k) = ( % True if column k of the key belongs to the initial key at round i
      (KEY_BITS==128 /\ i==0 )   % AES 128 : first round
  \/  (KEY_BITS==192 /\ i*4+k<6) % AES 192: First round and two first columns of second round
  \/  (KEY_BITS==256 /\ i<2)     % AES 256: Two first rounds
);
test isSBRound(int:i) = ( % True if a column of the key goes through SBoxes at round i
      (KEY_BITS==128) %AES 128 : all rounds
  \/  (KEY_BITS==192 /\ i mod 3 != 0) % AES 192: Rounds that are not multiples of 3
  \/  (KEY_BITS==256 /\ i>0) %AES 256: All rounds except the first one
);
test isSBOutput(int: i, int: k) = ( % False if > 3 and no SB at this round
       not(
	     k>3 /\
	     (  (KC==6 /\ i mod 3 == 0) 
		      \/ (KC==8 /\ i==0) 
	     ) 
	)
);
function int: getNbCol (int: i) = ( % Returns the index of the column that goes through a SBox at round i. Precondition: isSBRound(i) is true
     if KEY_BITS in {128,256} then 3 % AES 128 and 256
     elseif i mod 3==1 then 1        % AES 192, case 1
     else 3                          % AES 192, case 2
     endif 
);


function int: numCol (int: a, int: b, int: c) = 4;

