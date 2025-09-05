function onPageLoad(gameId) {
    new EventSource(`/game/${gameId}/events?gameId=${gameId}`)
        .addEventListener('update', function (event) {
            const updatedGameId = event.data;
            if (updatedGameId === gameId) location.reload();
            else location.assign(`/game/${updatedGameId}`)
        });
}