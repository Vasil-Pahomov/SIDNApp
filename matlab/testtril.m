x=[0 1 1 0 0];
y=[0 1 0 1 0];
z=[1 1 1 1 0];
NP=length(x);
px=0.4;py=0.5;pz=1;

re = 0.0;

r = sqrt((x-px).^2 + (y-py).^2 + (z-pz).^2) + re;

B= [x;y;z;-r;]';

a = zeros(NP,1);
p = zeros(NP,4);

for i=1:NP
    p(i,:) = [x(i) y(i) z(i) r(i)];
    a(i) = 0.5 * lip(p(i,:), p(i,:));
end

Bp = pinv(B);
e = ones(NP,1);

ea=lip(Bp*e,Bp*e);
eb=2*(lip(Bp*e,Bp*a)-1);
ec=lip(Bp*a,Bp*a);

L1=(-eb + sqrt(eb^2-4*ea*ec))/(2*ea);
L2=(-eb - sqrt(eb^2-4*ea*ec))/(2*ea);

ro1=Bp*a + L1*e
ro2=Bp*a + L2*e

