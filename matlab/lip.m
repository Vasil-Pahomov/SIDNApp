function l = lip(a,b)

  if (length(a) ~= 4 && length(b) ~= 4)
      error('Argument must be of length 4');
  end;
  
  l = a(1)*b(1) + a(2)*b(2) + a(3)*b(3) - a(4)*b(4);
  
end
