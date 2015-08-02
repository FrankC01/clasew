on cleanval(val)
	local oval
	set oval to null
	if val is not equal to missing value then
		set oval to val
	end if
	return oval
end cleanval
