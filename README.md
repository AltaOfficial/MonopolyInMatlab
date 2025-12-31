<img width="1919" height="1079" alt="image" src="https://github.com/user-attachments/assets/5b39d426-1608-4247-a558-afd841b5f737" />

# Monopoly in MATLAB — How to Run

> **⚠️ Note:** You cannot run this in MATLAB Online.

To play the game, **you do not need to download the server files**, as long as  
`monopolyinmatlabserver-production.up.railway.app` is accessible and not returning a `404`.

---

## How to Run
1. Locate the `play.m` file.  
2. Run `play.m` to start the game.

---

## `dev_mode` Explanation
Inside `play.m` there is a setting called `dev_mode`.

- **`dev_mode = false` (default)**  
  Connects to the *production server*.  
  Use this if you are just trying to play the game normally.

- **`dev_mode = true`**  
  Connects to a **local server** at `ws://localhost:8000/ws`.  
  Only set this to `true` if you have downloaded and are running the server on your own machine.

---

## First Launch Behavior
- On the first run, the game may cause MATLAB to restart.  
- If MATLAB does **not** reopen automatically, start MATLAB manually and run `play.m` again.  
- After the initial launch, it should run normally.

---

## Performance Note
If you’re using a VM or a slow PC, loading may take longer — please be patient.
