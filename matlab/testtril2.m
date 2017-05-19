clc;
clear;
x=[0 5 5 0];
y=[0 0 3 3];
z=[1 1 1 1];
NP=length(x);
px=1.43;py=1.51;pz=1;

re =[-0.037034125369143675 0.003989668329131946 -0.042274998625716834 0.03971478964254062];

r = sqrt((x-px).^2 + (y-py).^2 + (z-pz).^2) + re;

i0=ones(NP,1);

for i=1:NP
    A(i,:)=[x(i);y(i);z(i);r(i)]';
    r(i) = 0.5*lip(A(i,:),A(i,:));
end;
r=r';

B=inv(A'*A)*A';
u=B*i0;
v=B*r;
E=lip(u,u);
F=lip(u,v)-1
G=lip(v,v);

4*F^2-4*E*G

l1=(-2*F+sqrt(4*F^2-4*E*G))/(2*E);
l2=(-2*F-sqrt(4*F^2-4*E*G))/(2*E);

y1=l1*u+v;
y2=l2*u+v;

y1
y2


