(function () {
    var token = localStorage.getItem('token');
    if (!token) return;
    try {
        var base64Url = token.split('.')[1];
        if (!base64Url) return;
        var base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
        var json = decodeURIComponent(
            atob(base64)
                .split('')
                .map(function (c) {
                    return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
                })
                .join('')
        );
        var payload = JSON.parse(json);
        var roles = payload.roles || [];
        var staffRoles = ['ROLE_ADMIN', 'ROLE_PM', 'ROLE_OM', 'ROLE_ORDER_MANAGER'];
        var isStaff = roles.some(function (r) {
            return staffRoles.indexOf(r) !== -1;
        });
        if (isStaff) {
            alert(
                'The online store is for customers only. Use the home page for product and order management.'
            );
            window.location.replace('../index.html');
        }
    } catch (e) {
        /* ignore */
    }
})();
