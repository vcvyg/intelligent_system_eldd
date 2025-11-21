function logout() {
    localStorage.removeItem('userInfo');
    window.location.href = 'login.html';}