%codes digital signal to BPSK
%dig: digital sequence to code (array of 0's and 1's)
%fs: sampling frequency
%fc: carrier frequency
%SC: number of carrier periods per single digit
%use_sin: nonzero indicates to use sin instead of cos (quadrature
%component)
function signal = bpsk(dig, fs, fc, SC, use_sin)
T=SC*length(dig)/fc;
t=0:1/fs:(T-1/fs);
if (use_sin) 
    signal=sin(2*pi*fc*t+pi*dig(1+floor(t*fc/SC)));
else
    signal=cos(2*pi*fc*t+pi*dig(1+floor(t*fc/SC)));%.*abs(dig(1+floor(t*fc/SC)));
end
