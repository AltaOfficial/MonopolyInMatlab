function displayError(errorLabel, errorText, time)
%DISPLAYERROR Summary of this function goes here
%   Display an error using a passed in label
arguments (Input)
    errorLabel
    errorText
    time = 3
end
    errorLabel.Text = errorText;
    errorLabel.Visible = "on";
    pause(time);
    errorLabel.Visible = "on";
end